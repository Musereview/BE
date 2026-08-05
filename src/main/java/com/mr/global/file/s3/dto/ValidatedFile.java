package com.mr.global.file.s3.dto;

// S3 업로드 완료 검증 결과
public record ValidatedFile(
        String objectKey,
        String fileUrl,
        Long fileSize,
        String contentType
) {
}
