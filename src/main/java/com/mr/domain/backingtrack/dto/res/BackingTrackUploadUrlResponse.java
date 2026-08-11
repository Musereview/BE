package com.mr.domain.backingtrack.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mr.global.file.s3.dto.PresignedUrlUpload;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

public record BackingTrackUploadUrlResponse(
        String objectKey,
        String uploadUrl,

        @Schema(description = "URL 만료 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
        Instant expiresAt,

        Map<String, String> requiredHeaders
) {
    public static BackingTrackUploadUrlResponse from(
            PresignedUrlUpload presignedUpload
    ) {
        return new BackingTrackUploadUrlResponse(
                presignedUpload.objectKey(),
                presignedUpload.uploadUrl(),
                presignedUpload.expiresAt(),
                presignedUpload.requiredHeaders()
        );
    }
}