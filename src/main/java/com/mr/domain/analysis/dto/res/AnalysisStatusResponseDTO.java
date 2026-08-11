package com.mr.domain.analysis.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record AnalysisStatusResponseDTO(
        Long analysisId,
        AnalysisStatus status,
        Integer progressRate,
        String message,

        @Schema(description = "생성 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
        Instant createdAt,

        @Schema(description = "완료 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
        Instant completedAt
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