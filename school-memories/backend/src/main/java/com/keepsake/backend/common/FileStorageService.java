package com.keepsake.backend.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores uploaded media on the local filesystem and exposes a public URL.
 * MVP keeps files local; swapping in S3/GCS later only changes this class.
 */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif",
            "video/mp4", "video/webm", "video/quicktime");

    private final Path root;

    public FileStorageService(@Value("${keepsake.upload-dir}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("No file provided");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType)) {
            throw ApiException.badRequest("Unsupported file type: " + contentType);
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename());
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0 && dot < original.length() - 1) {
            ext = original.substring(dot).toLowerCase();
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Files.createDirectories(root);
            Path target = root.resolve(filename).normalize();
            if (!target.startsWith(root)) {
                throw ApiException.badRequest("Invalid file name");
            }
            file.transferTo(target);
        } catch (IOException ex) {
            throw new ApiException(500, "Could not store file");
        }
        return "/uploads/" + filename;
    }

    public Path resolve(String filename) {
        return root.resolve(filename).normalize();
    }
}
