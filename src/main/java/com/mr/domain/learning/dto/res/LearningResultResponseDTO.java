package com.mr.domain.learning.dto.res;

import com.mr.domain.learning.entity.UserLearningProgress;

import java.time.LocalDateTime;

public class LearningResultResponseDTO {
    public record SaveResultResultDTO(
            Long userLearningProgressId,
            Long userId,
            Long learningId,
            String status,
            Integer score,
            LocalDateTime completedAt
    ) {
        public static SaveResultResultDTO from(UserLearningProgress progress) {
            return new SaveResultResultDTO(
                    progress.getId(),
                    progress.getUser().getUserId(),
                    progress.getLearning().getId(),
                    progress.getLearningStatus(),
                    progress.getScore(),
                    progress.getUpdatedAt()
            );
        }
    }
}
