package com.mr.domain.learning.dto.res;

import com.mr.domain.learning.entity.Learning;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class LearningAccompanimentListResponseDTO {

    @Schema(description = "실전 반주법 패키지(ACCOMPANIMENT) 전체보기 응답")
    public record AccompanimentListResultDTO(
            @Schema(description = "어컴패니먼트 패키지 전체 개수", example = "10")
            int totalCount,

            @Schema(description = "어컴패니먼트 패키지 전체 목록(제목 이름순)")
            List<AccompanimentItem> items
    ) {
        public static AccompanimentListResultDTO of(List<AccompanimentItem> items) {
            return new AccompanimentListResultDTO(items.size(), items);
        }
    }

    @Schema(description = "실전 반주법 패키지 항목")
    public record AccompanimentItem(
            @Schema(description = "패키지 ID", example = "5")
            Long learningId,

            @Schema(description = "패키지 제목", example = "Chapter 1")
            String title,

            @Schema(description = "패키지 설명", example = "코드에 자연스럽게 색채를 더하는 9th를 학습합니다.")
            String description,

            @Schema(description = "예상 소요 시간(분)", example = "10")
            Integer estimatedMinutes,

            @Schema(description = "진행률(%), 0~100", example = "100")
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
