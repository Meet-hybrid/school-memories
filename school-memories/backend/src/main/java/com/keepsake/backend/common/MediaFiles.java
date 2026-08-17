package com.keepsake.backend.common;

import java.util.Set;
import java.util.UUID;

import org.springframework.util.StringUtils;

/** Shared validation and naming rules for uploaded media (used by every storage backend). */
public final class MediaFiles {

    private static final Set<String> ALLOWED = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif",
            "video/mp4", "video/webm", "video/quicktime");

    private MediaFiles() {
    }

    public static boolean isImage(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    public static boolean isVideo(String contentType) {
        return contentType != null && contentType.startsWith("video/");
    }

    public static void checkType(String contentType) {
        if (contentType == null || !ALLOWED.contains(contentType)) {
            throw ApiException.badRequest("Unsupported file type: " + contentType);
        }
    }

    /** Random storage name that keeps a sane extension from the original filename. */
    public static String randomName(String originalFilename) {
        String original = StringUtils.cleanPath(originalFilename == null ? "upload" : originalFilename);
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0 && dot < original.length() - 1) {
            ext = original.substring(dot).toLowerCase();
        }
        return UUID.randomUUID().toString().replace("-", "") + ext;
    }
}
