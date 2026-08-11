package com.mr.domain.analysis.dto.res;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "분석 요청 생성 응답")
public record AnalysisCreateResponseDTO(
        @Schema(description = "생성된 분석 ID", example = "342")
        Long analysisId,

        @Schema(description = "분석 대상 연주 기록 ID", example = "128")
        Long playingId,

        @Schema(description = "분석 진행 상태 (생성 직후에는 PENDING)", example = "PENDING")
        AnalysisStatus status,

        @Schema(description = "분석 구간 시작 마디", example = "1")
        Integer startBar,

        @Schema(description = "분석 구간 종료 마디", example = "8")
        Integer endBar,

        @Schema(description = "분석 요청 시각", example = "2026-08-10T21:20:05")
        LocalDateTime createdAt
) {
    public static AnalysisCreateResponseDTO from(Analysis analysis) {
        return new AnalysisCreateResponseDTO(analysis.getId(), analysis.getPlaying().getId(), analysis.getStatus(),
                analysis.getStartBar(), analysis.getEndBar(), analysis.getCreatedAt());
    }
}
