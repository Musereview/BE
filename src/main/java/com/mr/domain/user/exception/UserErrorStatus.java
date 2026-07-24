package com.mr.domain.user.exception;

import com.mr.global.apipayload.code.BaseCode;
import org.springframework.http.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserErrorStatus implements BaseCode {

    NICKNAME_REQUIRED(HttpStatus.BAD_REQUEST, "USER_400_01", "닉네임은 필수입니다."),
    NICKNAME_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "USER_400_02", "닉네임은 한글, 영어, 숫자 2~10자로 입력해야 합니다."),

    private final HttpStatus status;
    private final String code;
    private final String message;
}
