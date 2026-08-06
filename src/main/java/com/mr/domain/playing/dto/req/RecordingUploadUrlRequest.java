package com.mr.domain.playing.dto.req;

import com.mr.global.file.s3.dto.FileUploadCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RecordingUploadUrlRequest(
        @NotBlank(message = "파일명은 필수입니다.")
        String fileName,

        @NotBlank(message = "Content-Type은 필수입니다.")
        String contentType,

        @NotNull(message = "파일 크기는 필수입니다.")
        @Positive(message = "파일 크기는 0보다 커야 합니다.")
        Long fileSize
) {
    public FileUploadCommand toCommand() {
        return new FileUploadCommand(
                fileName,
                contentType,
                fileSize
        );
    }
}
