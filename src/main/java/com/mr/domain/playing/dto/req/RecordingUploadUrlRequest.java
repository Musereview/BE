package com.mr.domain.playing.dto.req;

import com.mr.global.file.s3.dto.FileUploadCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RecordingUploadUrlRequest(

        @Schema(description = "업로드할 녹음 파일명", example = "recording.webm")
        @NotBlank(message = "파일명은 필수입니다.")
        String fileName,

        @Schema(description = "업로드할 녹음 파일의 Content-Type", example = "audio/webm")
        @NotBlank(message = "Content-Type은 필수입니다.")
        String contentType,

        @Schema(description = "업로드할 녹음 파일 크기(byte)", example = "1048576")
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
