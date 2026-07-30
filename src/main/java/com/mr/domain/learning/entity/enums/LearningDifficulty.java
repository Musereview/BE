package com.mr.domain.learning.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LearningDifficulty {
    BEGINNER("입문"),
    INTERMEDIATE("중급"),
    ADVANCED("고급");

    private final String label;
}
