package com.keepsake.backend.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores uploaded media on the local filesystem and exposes it via the
 * {@code /uploads/**} handler (see {@code WebConfig}). Files live under
 * {@code keepsake.upload-dir}.
 */
@Service
@ConditionalOnProperty(name = "keepsake.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path root;

    public LocalStorageService(@Value("${keepsake.upload-dir}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("No file provided");
        }
        MediaFiles.checkType(file.getContentType());
        String filename = MediaFiles.randomName(file.getOriginalFilename());
        try {
            Files.createDirectories(root);
            Path target = safeTarget(filename);
            file.transferTo(target);
        } catch (IOException ex) {
            throw new ApiException(500, "Could not store file");
        }
        return "/uploads/" + filename;
    }

    @Override
    public String store(byte[] bytes, String contentType, String filename) {
        String name = MediaFiles.randomName(filename);
        try {
            Files.createDirectories(root);
            Path target = safeTarget(name);
            Files.write(target, bytes);
        } catch (IOException ex) {
            throw new ApiException(500, "Could not store file");
        }
        return "/uploads/" + name;
    }

    private Path safeTarget(String filename) {
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw ApiException.badRequest("Invalid file name");
        }
        return target;
    }
}
