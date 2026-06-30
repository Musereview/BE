package com.mr.global.apiPayLoad.code;

import org.springframework.http.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommonStatus implements BaseCode {

    // 성공 응답
    SUCCESS(HttpStatus.OK, "COMMON_200", "요청에 성공하였습니다."),

    // 도메인 에러코드 예시
    TOKEN_MISSING(HttpStatus.BAD_REQUEST, "AUTH_400_07", "토큰 값이 필요합니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500_01", "서버 에러가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}