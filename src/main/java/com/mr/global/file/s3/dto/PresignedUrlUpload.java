package com.mr.global.file.s3.dto;

import java.time.Instant;
import java.util.Map;

// S3 Presigned URL 발급 결과
public record PresignedUrlUpload(
        String objectKey,
        String uploadUrl,
        Instant expiresAt,
        Map<String, String> requiredHeaders
) {
}
