package com.mr.global.file.s3.dto.res;

public record RecordingUploadCompleteResponse(
        String objectKey,
        Long fileSize,
        String contentType
) {
}
