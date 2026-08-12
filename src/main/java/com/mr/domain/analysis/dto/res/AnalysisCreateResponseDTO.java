package com.mr.domain.analysis.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

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

        @Schema(description = "분석 요청 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
        Instant createdAt
) {
    public static AnalysisCreateResponseDTO from(Analysis analysis) {
        return new AnalysisCreateResponseDTO(analysis.getId(), analysis.getPlaying().getId(), analysis.getStatus(),
                analysis.getStartBar(), analysis.getEndBar(), analysis.getCreatedAt());
    }
}
