package com.keepsake.backend.common;

import org.springframework.web.multipart.MultipartFile;

/**
 * Where uploaded media lives. Local filesystem and S3-compatible object
 * storage are both implementations; the active one is chosen by
 * {@code keepsake.storage.type} (local | s3).
 */
public interface StorageService {

    /**
     * Validates and stores an uploaded file.
     *
     * @return the public URL of the stored object (absolute for object storage,
     *         relative like {@code /uploads/...} for local disk)
     */
    String store(MultipartFile file);

    /**
     * Stores already-processed bytes (e.g. a generated thumbnail) under a fresh name.
     *
     * @return the public URL of the stored object
     */
    String store(byte[] bytes, String contentType, String filename);
}
