package com.mr.domain.learning.dto.res;

import com.mr.domain.learning.entity.Learning;
import com.mr.domain.learning.entity.LearningStep;

import java.util.List;

public class LearningCurriculumResponseDTO {

    public record CurriculumResultDTO(
            Long learningId,
            String title,
            String subtitle,
            String difficulty,
            String theoryContent,
            String practiceTip,
            ProgressInfo progress,
            List<StepItem> steps
    ) {
        public static CurriculumResultDTO of(Learning learning, ProgressInfo progress, List<StepItem> steps) {
            return new CurriculumResultDTO(
                    learning.getId(),
                    learning.getTitle(),
                    learning.getSummary(),
                    learning.getDifficulty().name(),
                    learning.getContent(),
                    learning.getPracticeTip(),
                    progress,
                    steps
            );
        }
    }

    public record ProgressInfo(
            Integer completedStepCount,
            Integer totalStepCount,
            Integer progressRate
    ) {
        public static ProgressInfo of(long completedStepCount, long totalStepCount) {
            int progressRate = totalStepCount == 0
                    ? 0
                    : (int) Math.round((double) completedStepCount / totalStepCount * 100);
            return new ProgressInfo((int) completedStepCount, (int) totalStepCount, progressRate);
        }
    }

    public record StepItem(
            Long learningStepId,
            Integer stepNo,
            String title,
            String description,
            Integer estimatedMinutes,
            String status,
            Integer score
    ) {
        public static StepItem of(LearningStep step, String status, Integer score) {
            return new StepItem(
                    step.getId(),
                    step.getStepNo(),
                    step.getTitle(),
                    step.getSummary(),
                    step.getEstimatedMinutes(),
                    status,
                    score
            );
        }
    }
}
