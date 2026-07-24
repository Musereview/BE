package com.mr.domain.learning.exception;

import com.mr.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LearningErrorStatus implements BaseCode {

    INVALID_LEARNING_STEP(HttpStatus.BAD_REQUEST, "LEARNING_400_01", "해당 학습 단계는 지칭한 학습(Learning)에 속하지 않습니다."),
    LEARNING_NOT_FOUND(HttpStatus.NOT_FOUND, "LEARNING_404_01", "해당 학습(Learning)을 찾을 수 없습니다."),
    LEARNING_STEP_NOT_FOUND(HttpStatus.NOT_FOUND, "LEARNING_404_02", "해당 학습 단계(Step)를 찾을 수 없습니다."),
    PLAYING_EXAMPLE_NOT_FOUND(HttpStatus.NOT_FOUND, "LEARNING_404_03", "해당 학습 단계에 등록된 실습 데이터를 찾을 수 없습니다."),
    INVALID_DIFFICULTY(HttpStatus.BAD_REQUEST, "LEARNING_400_02", "difficulty 값이 없거나 지원하지 않는 값입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
