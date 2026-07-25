package com.mr.domain.learning.dto.res;

import com.mr.domain.learning.entity.Learning;

import java.util.List;

public class LearningAccompanimentListResponseDTO {

    public record AccompanimentListResultDTO(
            int totalCount,
            List<AccompanimentItem> items
    ) {
        public static AccompanimentListResultDTO of(List<AccompanimentItem> items) {
            return new AccompanimentListResultDTO(items.size(), items);
        }
    }

    public record AccompanimentItem(
            Long learningId,
            String title,
            String description,
            Integer estimatedMinutes,
            Integer progressRate
    ) {
        public static AccompanimentItem of(Learning learning, int progressRate) {
            return new AccompanimentItem(
                    learning.getId(),
                    learning.getTitle(),
                    learning.getSummary(),
                    learning.getEstimatedMinutes(),
                    progressRate
            );
        }
    }
}
