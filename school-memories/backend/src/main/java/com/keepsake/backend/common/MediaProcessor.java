package com.keepsake.backend.common;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * The upload pipeline: stores the original via {@link StorageService}, then
 * generates a lightweight preview — a JPEG thumbnail for photos (ImageIO, no
 * extra dependencies) and a poster frame for videos when FFmpeg is available
 * ({@code keepsake.media.ffmpeg-path}). Preview generation is best-effort:
 * failures degrade to "no thumbnail" rather than rejecting the upload.
 */
@Component
public class MediaProcessor {

    private static final Logger log = LoggerFactory.getLogger(MediaProcessor.class);
    private static final int THUMB_MAX_WIDTH = 600;
    private static final long FFMPEG_TIMEOUT_SECONDS = 20;

    private final StorageService storage;
    private final String ffmpegPath;

    public MediaProcessor(StorageService storage,
                          @Value("${keepsake.media.ffmpeg-path:}") String ffmpegPath) {
        this.storage = storage;
        this.ffmpegPath = ffmpegPath == null ? "" : ffmpegPath.trim();
    }

    /** @return null when no file was provided */
    public ProcessedMedia process(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new ApiException(500, "Could not read file");
        }
        String url = storage.store(file);
        String contentType = file.getContentType();
        boolean video = MediaFiles.isVideo(contentType);
        String thumbnailUrl = video ? videoPoster(bytes) : imageThumbnail(bytes, contentType);
        return new ProcessedMedia(url, video ? "VIDEO" : "PHOTO", thumbnailUrl);
    }

    private String imageThumbnail(byte[] bytes, String contentType) {
        if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
            return null; // webp/gif left as-is
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null || image.getWidth() <= THUMB_MAX_WIDTH) {
                return null; // already small enough; the original doubles as its own preview
            }
            int height = (int) Math.round(image.getHeight() * (THUMB_MAX_WIDTH / (double) image.getWidth()));
            BufferedImage scaled = new BufferedImage(THUMB_MAX_WIDTH, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(image, 0, 0, THUMB_MAX_WIDTH, height, null);
            g.dispose();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(scaled, "jpg", out);
            return storage.store(out.toByteArray(), "image/jpeg", "thumb.jpg");
        } catch (Exception ex) {
            log.warn("Could not generate image thumbnail", ex);
            return null;
        }
    }

    private String videoPoster(byte[] bytes) {
        if (ffmpegPath.isBlank()) {
            return null;
        }
        Path in = null;
        Path out = null;
        try {
            in = Files.createTempFile("keepsake-video", ".bin");
            Files.write(in, bytes);
            out = Files.createTempFile("keepsake-poster", ".jpg");
            Process process = new ProcessBuilder(ffmpegPath, "-y", "-i", in.toString(),
                    "-frames:v", "1", "-vf", "scale=" + THUMB_MAX_WIDTH + ":-2", "-q:v", "3",
                    out.toString())
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(FFMPEG_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return null;
            }
            if (process.exitValue() != 0 || Files.size(out) == 0) {
                return null;
            }
            return storage.store(Files.readAllBytes(out), "image/jpeg", "poster.jpg");
        } catch (Exception ex) {
            log.warn("Could not generate video poster (is ffmpeg installed?)", ex);
            return null;
        } finally {
            deleteQuietly(in);
            deleteQuietly(out);
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best effort
        }
    }

    /** The stored original plus an optional lightweight preview. */
    public record ProcessedMedia(String url, String type, String thumbnailUrl) {
    }
}
