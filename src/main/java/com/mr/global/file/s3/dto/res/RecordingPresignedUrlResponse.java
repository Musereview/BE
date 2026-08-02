package com.mr.global.file.s3.dto.res;

import java.time.Instant;
import java.util.Map;

public record RecordingPresignedUrlResponse(
        String objectKey,
        String uploadUrl,
        Instant expiresAt,
        Map<String, String> requiredHeaders
) {
}
