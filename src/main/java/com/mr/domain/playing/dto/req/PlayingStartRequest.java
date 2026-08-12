package com.mr.domain.playing.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PlayingStartRequest(

        @Schema(description = "연주에 사용할 백킹트랙 ID", example = "25")
        @NotNull(message = "백킹트랙 ID는 필수입니다.")
        @Min(value = 1, message = "백킹트랙 ID는 1 이상이어야 합니다.")
        Long backingTrackId
) {
}
