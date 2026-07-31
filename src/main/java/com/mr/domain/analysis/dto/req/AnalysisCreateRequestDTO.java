package com.mr.domain.analysis.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AnalysisCreateRequestDTO(
        @NotNull Long playingId,
        @NotNull @Positive Integer startBar,
        @NotNull @Positive Integer endBar
) {
}
