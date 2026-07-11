package com.mr.domain.auth.entity.enums;

import com.mr.global.apipayload.code.BaseCode;
import org.springframework.http.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthErrorStatus implements BaseCode {

    INVALID_AUTH_REQUEST(HttpStatus.BAD_REQUEST, "AUTH_400_01", "인증 관련 필수 입력값이 올바르지 않거나 누락되었습니다."),
    TOKEN_MISSING(HttpStatus.BAD_REQUEST, "AUTH_400_02", "토큰 값이 필요합니다."),
    INVALID_TOKEN_EXPIRY(HttpStatus.BAD_REQUEST, "AUTH_400_03", "만료 시간은 현재 시간 이후여야 합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_01", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_02", "만료된 토큰입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}