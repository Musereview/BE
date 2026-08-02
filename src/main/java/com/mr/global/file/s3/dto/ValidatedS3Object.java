package com.mr.global.file.s3.dto;

public record ValidatedS3Object(
        String objectKey,
        String fileUrl,
        Long fileSize,
        String contentType
) {
}
