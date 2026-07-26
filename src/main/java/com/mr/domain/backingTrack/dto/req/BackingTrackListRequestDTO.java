package com.mr.domain.backingTrack.dto.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class BackingTrackListRequestDTO {

    public record ListRequestDTO(
            String sort,
            String genre,
            String keySignature,
            String scaleType,
            Integer bpmMin,
            Integer bpmMax,

            @Min(value = 0, message = "BACKING_TRACK_400_26")
            Integer page,

            @Min(value = 1, message = "BACKING_TRACK_400_27")
            @Max(value = 9, message = "BACKING_TRACK_400_27")
            Integer size
    ) {
        public ListRequestDTO {
            if (page == null) page = 0;
            if (size == null) size = 9;
        }
    }
}
