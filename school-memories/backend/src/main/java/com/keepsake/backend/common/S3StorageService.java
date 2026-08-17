package com.keepsake.backend.common;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;

/**
 * Stores uploaded media in any S3-compatible object store (AWS S3, MinIO,
 * DigitalOcean Spaces, …) via the MinIO client. Public URLs are built from
 * {@code keepsake.storage.s3.public-base-url} when set (e.g. a CDN), otherwise
 * from the endpoint + bucket (path-style).
 *
 * Enabled with {@code keepsake.storage.type=s3}.
 */
@Service
@ConditionalOnProperty(name = "keepsake.storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final MinioClient client;
    private final String bucket;
    private final String publicBaseUrl;

    public S3StorageService(
            @Value("${keepsake.storage.s3.endpoint:}") String endpoint,
            @Value("${keepsake.storage.s3.region:}") String region,
            @Value("${keepsake.storage.s3.access-key:}") String accessKey,
            @Value("${keepsake.storage.s3.secret-key:}") String secretKey,
            @Value("${keepsake.storage.s3.bucket:}") String bucket,
            @Value("${keepsake.storage.s3.public-base-url:}") String publicBaseUrl) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("keepsake.storage.s3.bucket must be set when keepsake.storage.type=s3");
        }
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(endpoint == null || endpoint.isBlank() ? "https://s3.amazonaws.com" : endpoint)
                .credentials(accessKey == null ? "" : accessKey, secretKey == null ? "" : secretKey);
        if (region != null && !region.isBlank()) {
            builder.region(region);
        }
        this.client = builder.build();
        this.bucket = bucket;
        this.publicBaseUrl = (publicBaseUrl == null || publicBaseUrl.isBlank()) ? null : publicBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("No file provided");
        }
        MediaFiles.checkType(file.getContentType());
        String name = MediaFiles.randomName(file.getOriginalFilename());
        try (InputStream in = file.getInputStream()) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket).object(name)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception ex) {
            throw new ApiException(500, "Could not store file");
        }
        return publicUrl(name);
    }

    @Override
    public String store(byte[] bytes, String contentType, String filename) {
        String name = MediaFiles.randomName(filename);
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket).object(name)
                    .stream(in, bytes.length, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception ex) {
            throw new ApiException(500, "Could not store file");
        }
        return publicUrl(name);
    }

    private String publicUrl(String name) {
        if (publicBaseUrl != null) {
            return publicBaseUrl + "/" + name;
        }
        return "https://s3.amazonaws.com/" + bucket + "/" + name;
    }
}
