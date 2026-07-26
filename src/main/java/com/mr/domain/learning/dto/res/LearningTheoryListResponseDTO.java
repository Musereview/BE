package com.mr.domain.learning.dto.res;

import com.mr.domain.learning.entity.Learning;

import java.util.List;

public class LearningTheoryListResponseDTO {

    public record TheoryListResultDTO(
            List<TheoryItem> items
    ) {
        public static TheoryListResultDTO from(List<Learning> learnings) {
            List<TheoryItem> items = learnings.stream()
                    .map(TheoryItem::from)
                    .toList();
            return new TheoryListResultDTO(items);
        }
    }

    public record TheoryItem(
            Long learningId,
            String title,
            String difficulty,
            String summary
    ) {
        public static TheoryItem from(Learning learning) {
            return new TheoryItem(
                    learning.getId(),
                    learning.getTitle(),
                    learning.getDifficulty().name(),
                    learning.getSummary()
            );
        }
    }
}
