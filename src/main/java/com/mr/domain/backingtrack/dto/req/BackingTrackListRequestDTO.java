package com.mr.domain.backingtrack.dto.req;

import jakarta.validation.constraints.Positive;

public class BackingTrackListRequestDTO {

    public record ListRequestDTO(
            @Positive(message = "BACKING_TRACK_400_29")
            Long cursor   // 마지막으로 받은 트랙의 id, 첫 요청 시 null
    ) {
    }
}
