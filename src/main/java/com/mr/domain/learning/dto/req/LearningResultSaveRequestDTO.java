package com.mr.domain.learning.dto.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class LearningResultSaveRequestDTO {
    public record SaveResultDTO(
            @NotNull(message = "점수는 필수 입력값입니다.")
            @Min(value = 0, message = "점수는 0점 이상이어야 합니다.")
            @Max(value = 100, message = "점수는 100점 이하여야 합니다.")
            Integer score,

            @NotNull(message = "학습 스텝 ID는 필수 입력값입니다.")
            Long learningStepId
    ) {}
}
