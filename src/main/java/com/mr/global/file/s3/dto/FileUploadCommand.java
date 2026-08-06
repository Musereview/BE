package com.mr.global.file.s3.dto;

/**
 * S3 Presigned URL 발급에 필요한 범용 파일 업로드 정보
 *
 * HTTP 요청 DTO에 직접 의존하지 않도록
 * S3 모듈 내부에서 사용하는 Command 객체로 분리
 */
public record FileUploadCommand(
        String originalFileName,
        String contentType,
        long fileSize
) {
}
