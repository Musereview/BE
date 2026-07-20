package com.mr.domain.user.entity.enums;

import com.mr.global.apipayload.code.BaseCode;
import org.springframework.http.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserErrorStatus implements BaseCode {

    // 닉네임 미입력
    NICKNAME_REQUIRED(HttpStatus.BAD_REQUEST, "USER_400_01", "닉네임은 필수입니다."),

    // 닉네임 길이 초과
    NICKNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "USER_400_02", "닉네임은 10자를 초과할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
