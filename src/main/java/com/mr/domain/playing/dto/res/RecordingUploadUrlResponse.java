package com.mr.domain.playing.dto.res;

import com.mr.global.file.s3.dto.PresignedUrlUpload;
import java.time.Instant;
import java.util.Map;

public record RecordingUploadUrlResponse(
        String objectKey,
        String uploadUrl,
        Instant expiresAt,
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
