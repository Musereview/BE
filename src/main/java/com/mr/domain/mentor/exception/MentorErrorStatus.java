package com.mr.domain.mentor.exception;

import com.mr.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MentorErrorStatus implements BaseCode {

    MENTOR_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "MENTOR_400_01", "필수 정보가 누락되었습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
