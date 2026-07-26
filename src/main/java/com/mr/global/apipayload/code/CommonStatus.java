package com.mr.global.apipayload.code;

import org.springframework.http.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommonStatus implements BaseCode {

    // 공통 성공 / 실패
    SUCCESS(HttpStatus.OK, "COMMON_200", "요청에 성공하였습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_400_01", "입력값이 올바르지 않습니다."),
    HTTP_MESSAGE_NOT_READABLE(HttpStatus.BAD_REQUEST, "COMMON_400_02", "요청 본문(JSON) 파싱에 실패했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500_01", "서버 에러가 발생했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_401_01", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_403_01", "금지된 접근입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}