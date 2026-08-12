package com.mr.domain.playing.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mr.global.file.s3.dto.PresignedUrlUpload;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

public record RecordingUploadUrlResponse(

        @Schema(
                description = "업로드할 녹음 파일의 S3 Object Key",
                example = "recordings/34/2026-08-12/161800_a1b2c3.webm"
        )
        String objectKey,

        @Schema(
                description = "녹음 파일 업로드용 Presigned URL",
                example = "https://example.com/presigned-upload-url"
        )
        String uploadUrl,

        @Schema(description = "URL 만료 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
        Instant expiresAt,

        @Schema(description = "S3 업로드 요청 시 포함해야 하는 필수 헤더")
        Map<String, String> requiredHeaders
) {
    public static RecordingUploadUrlResponse from(
            PresignedUrlUpload presignedUpload
    ) {
        return new RecordingUploadUrlResponse(
                presignedUpload.objectKey(),
                presignedUpload.uploadUrl(),
                presignedUpload.expiresAt(),
                presignedUpload.requiredHeaders()
        );
    }
}