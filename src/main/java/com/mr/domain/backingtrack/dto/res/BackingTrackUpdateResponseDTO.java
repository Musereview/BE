package com.mr.domain.backingtrack.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public class BackingTrackUpdateResponseDTO {

    public record UpdateResultDTO(
            Long backingTrackId,
            String title,

            @Schema(description = "수정 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
            Instant updatedAt
    ) {
        public static BackingTrackUpdateResponseDTO.UpdateResultDTO of(Long backingTrackId, String title, Instant updatedAt) {
            return new BackingTrackUpdateResponseDTO.UpdateResultDTO(backingTrackId, title, updatedAt);
        }
    }
}