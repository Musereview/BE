package com.mr.domain.learning.entity.enums;

import lombok.Getter;

@Getter
public enum LearningDifficulty {
    BEGINNER("입문"),
    INTERMEDIATE("중급"),
    ADVANCED("고급");

    private final String label;

    LearningDifficulty(String label) {
        this.label = label;
    }
}
