package com.mr.domain.backingtrack.dto.req;

public class BackingTrackListRequestDTO {

    public record ListRequestDTO(
            Long cursor   // 마지막으로 받은 트랙의 id, 첫 요청 시 null
    ) {
    }
}
