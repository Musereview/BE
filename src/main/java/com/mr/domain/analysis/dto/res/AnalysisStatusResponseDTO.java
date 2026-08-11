package com.mr.domain.analysis.dto.res;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "분석 상태 조회 응답")
public record AnalysisStatusResponseDTO(
        @Schema(description = "분석 ID", example = "342")
        Long analysisId,

        @Schema(description = "분석 진행 상태", example = "PROCESSING")
        AnalysisStatus status,

        @Schema(description = "진행률 (PENDING: 0, COMPLETED: 100, PROCESSING/FAILED: null)", example = "100")
        Integer progressRate,

        @Schema(description = "현재 상태 안내 메시지", example = "분석이 완료되었습니다.")
        String message,

        @Schema(description = "분석 요청 시각", example = "2026-08-10T21:20:05")
        LocalDateTime createdAt,

        @Schema(description = "분석 완료 시각. 완료 전이면 null", example = "2026-08-10T21:21:40")
        LocalDateTime completedAt
) {

    public static AnalysisStatusResponseDTO from(
            Analysis analysis,
            Integer progressRate,
            String message
    ) {
        return new AnalysisStatusResponseDTO(
                analysis.getId(),
                analysis.getStatus(),
                progressRate,
                message,
                analysis.getCreatedAt(),
                analysis.getCompletedAt()
        );
    }
}