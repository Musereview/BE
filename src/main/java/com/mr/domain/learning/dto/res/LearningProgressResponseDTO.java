package com.mr.domain.learning.dto.res;

public class LearningProgressResponseDTO {

    public record ProgressResultDTO(
            Long learningId,
            Integer progressRate
    ){
        public static ProgressResultDTO of(Long learningId, Integer progressRate){
            return new ProgressResultDTO(learningId, progressRate);
        }
    }
}
