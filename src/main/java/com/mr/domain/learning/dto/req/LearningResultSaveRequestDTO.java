package com.mr.domain.learning.dto.req;

import jakarta.validation.constraints.NotNull;

public class LearningResultSaveRequestDTO {
    public record SaveResultDTO(
            @NotNull(message = "점수는 필수 입력값입니다.")
            Integer score
    ) {}
}
