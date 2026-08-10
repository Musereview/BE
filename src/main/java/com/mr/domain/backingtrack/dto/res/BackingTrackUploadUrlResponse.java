package com.mr.domain.backingtrack.dto.res;

import com.mr.global.file.s3.dto.PresignedUrlUpload;

import java.time.Instant;
import java.util.Map;

public record BackingTrackUploadUrlResponse(
        String objectKey,
        String uploadUrl,
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
