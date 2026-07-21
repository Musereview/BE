package com.mr.domain.user.exception;

import com.mr.global.apipayload.code.BaseCode;
import org.springframework.http.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StudentErrorStatus implements BaseCode {

    USER_REQUIRED(HttpStatus.BAD_REQUEST, "STUDENT_400_01", "user는 필수입니다."),
    THEORY_LEVEL_REQUIRED(HttpStatus.BAD_REQUEST, "STUDENT_400_02", "theoryLevel은 필수입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
