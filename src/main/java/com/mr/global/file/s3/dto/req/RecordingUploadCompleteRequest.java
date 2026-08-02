package com.mr.global.file.s3.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RecordingUploadCompleteRequest(

        @NotBlank(message = "S3 Object Key는 필수입니다.")
        String objectKey,

        @NotBlank(message = "Content-Type은 필수입니다.")
        String contentType,

        @NotNull(message = "파일 크기는 필수입니다.")
        @Positive(message = "파일 크기는 0보다 커야 합니다.")
        Long fileSize
) {
}
