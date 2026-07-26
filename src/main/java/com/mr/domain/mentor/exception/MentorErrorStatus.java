package com.mr.domain.mentor.exception;

import com.mr.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MentorErrorStatus implements BaseCode {

    // 400_01/02는 스펙상 질문 전송 검증용으로 예약됨
    MENTOR_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "MENTOR_400_03", "필수 정보가 누락되었습니다."),

    MENTOR_SESSION_NOT_ACTIVE(HttpStatus.CONFLICT, "MENTOR_409_02", "활성 상태의 세션에서만 질문할 수 있습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
