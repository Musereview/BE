package com.mr.domain.mentor.exception;

import com.mr.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MentorErrorStatus implements BaseCode {

    MENTOR_QUESTION_REQUIRED(HttpStatus.BAD_REQUEST, "MENTOR_400_01", "질문 내용을 입력해주세요."),
    MENTOR_QUESTION_TOO_LONG(HttpStatus.BAD_REQUEST, "MENTOR_400_02", "질문은 500자 이하로 입력해주세요."),
    MENTOR_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "MENTOR_400_03", "필수 정보가 누락되었습니다."),

    MENTOR_ACCESS_DENIED(HttpStatus.FORBIDDEN, "MENTOR_403_01", "해당 대화에 접근할 수 없습니다."),
    MENTOR_ANALYSIS_NOT_COMPLETED(HttpStatus.CONFLICT, "MENTOR_409_01", "분석 완료 후 질문할 수 있습니다."),
    MENTOR_RESPONSE_IN_PROGRESS(HttpStatus.CONFLICT, "MENTOR_409_02", "이미 AI 멘토 답변을 생성하고 있습니다."),
    MENTOR_SESSION_NOT_ACTIVE(HttpStatus.CONFLICT, "MENTOR_409_03", "활성 상태의 세션에서만 질문할 수 있습니다."),
    MENTOR_QUESTION_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "MENTOR_429_01", "질문 가능 횟수를 초과했습니다."),
    MENTOR_RESPONSE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "MENTOR_500_01", "AI 멘토 답변 생성에 실패했습니다."),
    MENTOR_MESSAGE_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "MENTOR_500_02", "멘토 대화 저장에 실패했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
