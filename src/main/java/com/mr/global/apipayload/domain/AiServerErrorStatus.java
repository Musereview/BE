package com.mr.global.apipayload.domain;

import com.mr.global.apipayload.code.BaseCode;
import org.springframework.http.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AiServerErrorStatus implements BaseCode {

    INVALID_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "AI_SERVER_500_01", "AI 서버 응답을 처리하지 못했습니다."),
    RESPONSE_ERROR(HttpStatus.BAD_GATEWAY, "AI_SERVER_502_01", "AI 서버가 오류 응답을 반환했습니다."),
    CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVER_503_01", "AI 서버에 연결할 수 없습니다."),
    TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI_SERVER_504_01", "AI 서버 응답이 지연되고 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
