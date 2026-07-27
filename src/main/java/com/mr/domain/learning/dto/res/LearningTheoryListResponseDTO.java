package com.mr.domain.learning.dto.res;

import com.mr.domain.learning.entity.Learning;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class LearningTheoryListResponseDTO {

    @Schema(description = "학습 주제(THEORY) 전체보기 응답")
    public record TheoryListResultDTO(
            @Schema(description = "조건에 맞는 학습 주제 목록. 없으면 빈 배열")
            List<TheoryItem> items
    ) {
        public static TheoryListResultDTO from(List<Learning> learnings) {
            List<TheoryItem> items = learnings.stream()
                    .map(TheoryItem::from)
                    .toList();
            return new TheoryListResultDTO(items);
        }
    }

    @Schema(description = "학습 주제 항목")
    public record TheoryItem(
            @Schema(description = "패키지 ID", example = "2")
            Long learningId,

            @Schema(description = "패키지 제목", example = "Diatonic Chords")
            String title,

            @Schema(description = "패키지 난이도", example = "BEGINNER")
            String difficulty,

            @Schema(description = "패키지 요약 설명", example = "스케일 안에서 만들어지는 코드의 관계를 이해합니다.")
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
