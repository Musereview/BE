package com.mr.global.file.s3.dto.res;

public record RecordingUploadCompleteResponse(
        String recordingFileUrl,
        Long fileSize,
        String contentType
) {
}
