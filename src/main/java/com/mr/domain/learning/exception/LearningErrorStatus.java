package com.mr.domain.learning.exception;

import com.mr.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LearningErrorStatus implements BaseCode {

    INVALID_LEARNING_STEP(HttpStatus.BAD_REQUEST, "LEARNING_400_01", "해당 학습 단계는 지칭한 학습(Learning)에 속하지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
