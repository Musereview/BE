package com.mr.domain.backingtrack.dto.res;

public class PlayCountIncreaseResponseDTO {

    public record IncreaseResponseDTO(
            Long backingTrackId,
            Integer playCount
    ) {
        public static IncreaseResponseDTO of(Long backingTrackId, Integer playCount) {
            return new IncreaseResponseDTO(backingTrackId, playCount);
        }
    }
}
