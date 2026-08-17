package com.keepsake.backend.common;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaProcessorTest {

    @TempDir
    Path tempDir;

    private LocalStorageService storage() {
        return new LocalStorageService(tempDir.toString());
    }

    private static byte[] png(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    void large_photo_gets_a_thumbnail_no_wider_than_600px() throws Exception {
        LocalStorageService storage = storage();
        MediaProcessor processor = new MediaProcessor(storage, "");

        MediaProcessor.ProcessedMedia media = processor.process(
                new MockMultipartFile("file", "photo.png", "image/png", png(1200, 800)));

        assertThat(media.type()).isEqualTo("PHOTO");
        assertThat(media.url()).startsWith("/uploads/");
        assertThat(media.thumbnailUrl()).startsWith("/uploads/");

        // The thumbnail is an actual file on disk with the expected width.
        String name = media.thumbnailUrl().substring("/uploads/".length());
        BufferedImage thumb = ImageIO.read(Files.newInputStream(tempDir.resolve(name)));
        assertThat(thumb.getWidth()).isEqualTo(600);
        assertThat(thumb.getHeight()).isEqualTo(400);
    }

    @Test
    void small_photo_keeps_the_original_as_its_own_preview() throws Exception {
        MediaProcessor processor = new MediaProcessor(storage(), "");

        MediaProcessor.ProcessedMedia media = processor.process(
                new MockMultipartFile("file", "small.png", "image/png", png(300, 200)));

        assertThat(media.thumbnailUrl()).isNull();
    }

    @Test
    void video_gets_no_poster_without_ffmpeg_but_is_stored() throws Exception {
        MediaProcessor processor = new MediaProcessor(storage(), "");

        MediaProcessor.ProcessedMedia media = processor.process(
                new MockMultipartFile("file", "clip.mp4", "video/mp4", new byte[] {1, 2, 3, 4}));

        assertThat(media.type()).isEqualTo("VIDEO");
        assertThat(media.url()).startsWith("/uploads/");
        assertThat(media.thumbnailUrl()).isNull();
    }

    @Test
    void no_file_yields_no_media() {
        MediaProcessor processor = new MediaProcessor(storage(), "");
        assertThat(processor.process(null)).isNull();
        assertThat(processor.process(new MockMultipartFile("file", new byte[0]))).isNull();
    }

    @Test
    void unsupported_types_are_rejected() {
        MediaProcessor processor = new MediaProcessor(storage(), "");
        MockMultipartFile exe = new MockMultipartFile("file", "virus.exe", "application/x-msdownload", new byte[] {1});

        assertThatThrownBy(() -> processor.process(exe))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unsupported file type");
    }

    @Test
    void local_storage_stores_and_rejects_bad_types() throws Exception {
        LocalStorageService storage = storage();

        String url = storage.store(new MockMultipartFile("f", "a.jpg", "image/jpeg", new byte[] {1, 2, 3}));
        assertThat(url).startsWith("/uploads/");
        assertThat(Files.exists(tempDir.resolve(url.substring("/uploads/".length())))).isTrue();

        assertThatThrownBy(() -> storage.store(
                new MockMultipartFile("f", "a.txt", "text/plain", "hi".getBytes())))
                .isInstanceOf(ApiException.class);
    }
}
