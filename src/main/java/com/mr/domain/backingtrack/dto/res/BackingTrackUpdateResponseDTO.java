package com.mr.domain.backingtrack.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class BackingTrackUpdateResponseDTO {

    public record UpdateResultDTO(
            Long backingTrackId,
            String title,

            // 날짜 포맷
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime updatedAt
    ) {
        public static BackingTrackUpdateResponseDTO.UpdateResultDTO of(Long backingTrackId, String title, LocalDateTime updatedAt) {
            return new BackingTrackUpdateResponseDTO.UpdateResultDTO(backingTrackId, title, updatedAt);
        }
    }
}
