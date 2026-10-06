package com.example.hcc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService {

    private final S3Client s3Client;

    public void deleteFile(String s3Path) {
        if (s3Path == null || s3Path.isBlank()) {
            log.warn("S3 path is null or empty, skipping S3 deletion.");
            return;
        }

        try {
            String cleanPath = s3Path.replace("s3://", "");
            if (cleanPath.startsWith("/")) {
                cleanPath = cleanPath.substring(1);
            }
            int slashIndex = cleanPath.indexOf("/");
            if (slashIndex == -1) {
                log.warn("Invalid S3 path format: {}", s3Path);
                return;
            }

            String bucket = cleanPath.substring(0, slashIndex);
            String key = cleanPath.substring(slashIndex + 1);

            log.info("Deleting S3 object: bucket={}, key={}", bucket, key);
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Successfully deleted S3 object: {}", s3Path);
        } catch (Exception e) {
            log.error("Failed to delete S3 object for path {}: {}", s3Path, e.getMessage(), e);
            throw new RuntimeException("Failed to delete file from S3: " + e.getMessage(), e);
        }
    }
}
