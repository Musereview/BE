package com.mr.domain.auth.exception;

import com.mr.global.apipayload.code.BaseCode;
import org.springframework.http.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthErrorStatus implements BaseCode {

    // 입력 오류
    INVALID_AUTH_REQUEST(HttpStatus.BAD_REQUEST, "AUTH_400_01", "인증 관련 필수 입력값이 올바르지 않거나 누락되었습니다."),
    TOKEN_MISSING(HttpStatus.BAD_REQUEST, "AUTH_400_02", "토큰 값이 필요합니다."),
    INVALID_TOKEN_EXPIRY(HttpStatus.BAD_REQUEST, "AUTH_400_03", "만료 시간은 현재 시간 이후여야 합니다."),

    // 인증 실패 및 토큰 오류
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_01", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_02", "만료된 토큰입니다."),
    REVOKED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_03", "명시적으로 폐기 및 로그아웃 처리된 토큰입니다."),

    // 리소스 부재
    SOCIAL_AUTH_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_404_01", "해당 사용자의 소셜 인증 기록을 찾을 수 없습니다."),

    // 데이터 무결성
    ALREADY_LINKED_SOCIAL_ACCOUNT(HttpStatus.CONFLICT, "AUTH_409_01", "이미 다른 계정에 연동되어 있는 소셜 계정입니다."),

    // 외부 소셜 연동 오류
    OAUTH_CLIENT_ERROR(HttpStatus.UNAUTHORIZED, "AUTH_401_04", "소셜 로그인 인증에 실패했거나 유효하지 않은 소셜 액세스 토큰입니다."),
    OAUTH_SERVER_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_503_01", "소셜 인증 제공자(카카오/구글) 서버와의 통신에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}