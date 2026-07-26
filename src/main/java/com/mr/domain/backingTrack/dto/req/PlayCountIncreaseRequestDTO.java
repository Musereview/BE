package com.mr.domain.backingTrack.dto.req;

import jakarta.validation.constraints.NotNull;

public class PlayCountIncreaseRequestDTO {
    public record IncreaseRequestDTO(
            @NotNull(message = "AI 분석 결과 ID는 필수입니다.")
            Long analysisId
    ) {}
}
