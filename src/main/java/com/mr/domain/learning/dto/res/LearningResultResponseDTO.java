package com.mr.domain.learning.dto.res;

import com.mr.domain.learning.entity.UserLearningProgress;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public class LearningResultResponseDTO {
    @Schema(description = "학습 결과 저장 응답")
    public record SaveResultResultDTO(
            @Schema(description = "학습 진행 상태 ID", example = "124")
            Long userLearningProgressId,

            @Schema(description = "유저 ID", example = "5")
            Long userId,

            @Schema(description = "패키지 ID", example = "2")
            Long learningId,

            @Schema(description = "단계별 학습 상태 (NOT_STARTED / RETRY / COMPLETED)", example = "COMPLETED")
            String status,

            @Schema(description = "저장된 점수", example = "95")
            Integer score,

            @Schema(description = "저장/갱신 시각", type = "string", example = "2026-07-07T15:30:00Z")
            Instant completedAt
    ) {
        public static SaveResultResultDTO from(UserLearningProgress progress) {
            return new SaveResultResultDTO(
                    progress.getId(),
                    progress.getUser().getUserId(),
                    progress.getLearning().getId(),
                    progress.getLearningStatus(),
                    progress.getScore(),
                    progress.getLastStudiedAt()
            );
        }
    }
}
