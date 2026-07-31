package com.mr.domain.backingtrack.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class BackingTrackCreateResponseDTO {

    public record CreateResultDTO(
            Long backingTrackId,
            String title,

            // 날짜 포맷
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime createdAt
    ) {
        public static CreateResultDTO of(Long backingTrackId, String title, LocalDateTime createdAt) {
            return new CreateResultDTO(backingTrackId, title, createdAt);
        }
    }
}
