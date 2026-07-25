package com.mr.domain.backingTrack.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class BackingTrackSaveResponseDTO {

    public record SaveResultDTO(
            Long backingTrackId,
            String title,

            // 날짜 포맷
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime createdAt
    ) {
        public static SaveResultDTO of(Long backingTrackId, String title, LocalDateTime createdAt) {
            return new SaveResultDTO(backingTrackId, title, createdAt);
        }
    }
}
