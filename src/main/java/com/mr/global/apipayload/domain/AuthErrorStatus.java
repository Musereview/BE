package com.mr.global.apipayload.domain;

import com.mr.global.apipayload.code.BaseCode;
import org.springframework.http.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthErrorStatus implements BaseCode {

    // 예원님 도메인 에러코드 컨벤션 적용
    TOKEN_MISSING(HttpStatus.BAD_REQUEST, "AUTH_400_07", "토큰 값이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_01", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_02", "만료된 토큰입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}