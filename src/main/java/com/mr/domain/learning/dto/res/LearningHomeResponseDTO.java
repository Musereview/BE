package com.mr.domain.learning.dto.res;

import com.mr.domain.learning.entity.Learning;

import java.util.List;

public class LearningHomeResponseDTO {

    public record HomeResultDTO(
            CurrentLearning currentLearning,
            List<TheoryPackageItem> theoryPackages,
            List<LearningAccompanimentListResponseDTO.AccompanimentItem> accompanimentPackages
    ) {
        public static HomeResultDTO of(CurrentLearning currentLearning,
                                       List<TheoryPackageItem> theoryPackages,
                                       List<LearningAccompanimentListResponseDTO.AccompanimentItem> accompanimentPackages) {
            return new HomeResultDTO(currentLearning, theoryPackages, accompanimentPackages);
        }
    }

    public record CurrentLearning(
            Long learningId,
            String title,
            String difficulty,
            String stepTitle,
            Integer progressRate
    ) {
        public static CurrentLearning of(Learning learning, String stepTitle, int progressRate) {
            return new CurrentLearning(
                    learning.getId(),
                    learning.getTitle(),
                    learning.getDifficulty().name(),
                    stepTitle,
                    progressRate
            );
        }
    }

    public record TheoryPackageItem(
            Long learningId,
            String title,
            String difficulty,
            String summary
    ) {
        public static TheoryPackageItem from(Learning learning) {
            return new TheoryPackageItem(
                    learning.getId(),
                    learning.getTitle(),
                    learning.getDifficulty().name(),
                    learning.getSummary()
            );
        }
    }
}
