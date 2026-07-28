package com.mr.domain.learning.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

public class LearningProgressResponseDTO {

    @Schema(description = "학습 진행률 조회 응답")
    public record ProgressResultDTO(
            @Schema(description = "패키지 ID", example = "1")
            Long learningId,

            @Schema(description = "진행률(%), 0~100", example = "40")
            Integer progressRate
    ){
        public static ProgressResultDTO of(Long learningId, Integer progressRate){
            return new ProgressResultDTO(learningId, progressRate);
        }
    }
}
