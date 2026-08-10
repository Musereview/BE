package com.mr.domain.backingtrack.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public class BackingTrackUpdateResponseDTO {

    public record UpdateResultDTO(
            Long backingTrackId,
            String title,

            @JsonFormat(
                    shape = JsonFormat.Shape.STRING,
                    pattern = "yyyy-MM-dd'T'HH:mm:ss",
                    timezone = "UTC"
            )
            Instant updatedAt
    ) {
        public static BackingTrackUpdateResponseDTO.UpdateResultDTO of(Long backingTrackId, String title, Instant updatedAt) {
            return new BackingTrackUpdateResponseDTO.UpdateResultDTO(backingTrackId, title, updatedAt);
        }
    }
}
