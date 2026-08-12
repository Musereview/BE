package com.mr.domain.analysis.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "분석 요청 생성 요청")
public record AnalysisCreateRequestDTO(
        @Schema(description = "분석할 연주 기록 ID", example = "128", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Long playingId,

        @Schema(description = "분석 구간 시작 마디 (1 이상, 종료 마디 이하)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Positive Integer startBar,

        @Schema(description = "분석 구간 종료 마디 (1 이상, 연주 전체 마디 수 이하)", example = "8", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Positive Integer endBar
) {
}
