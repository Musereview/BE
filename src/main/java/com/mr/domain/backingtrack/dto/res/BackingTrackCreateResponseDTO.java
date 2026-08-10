package com.mr.domain.backingtrack.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public class BackingTrackCreateResponseDTO {

    public record CreateResultDTO(
            Long backingTrackId,
            String title,

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
            Instant createdAt
    ) {
        public static CreateResultDTO of(Long backingTrackId, String title, Instant createdAt) {
            return new CreateResultDTO(backingTrackId, title, createdAt);
        }
    }
}
