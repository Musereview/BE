package com.mr.domain.backingtrack.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public class BackingTrackCreateResponseDTO {

    public record CreateResultDTO(
            Long backingTrackId,
            String title,

            @Schema(description = "생성 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
            Instant createdAt
    ) {
        public static CreateResultDTO of(Long backingTrackId, String title, Instant createdAt) {
            return new CreateResultDTO(backingTrackId, title, createdAt);
        }
    }
}