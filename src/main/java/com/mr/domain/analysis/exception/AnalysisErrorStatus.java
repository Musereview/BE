package com.mr.domain.analysis.exception;

import com.mr.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AnalysisErrorStatus implements BaseCode {

    // 400_01, 400_02는 스펙상 마디 검증용으로 예약됨
    ANALYSIS_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "ANALYSIS_400_03", "필수 정보가 누락되었습니다."),

    ANALYSIS_OWNER_MISMATCH(HttpStatus.BAD_REQUEST,     "ANALYSIS_400_04", "playing 소유자와 user가 일치하지 않습니다."),

    ANALYSIS_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ANALYSIS_403_01", "해당 분석 결과에 접근할 수 없습니다."),

    ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "ANALYSIS_404_01", "분석 결과를 찾을 수 없습니다."),

    ANALYSIS_NOT_COMPLETED(HttpStatus.CONFLICT, "ANALYSIS_409_01", "아직 완료되지 않은 분석입니다."),

    INVALID_RAW_RESULT(HttpStatus.INTERNAL_SERVER_ERROR, "ANALYSIS_500_01", "저장된 분석 결과를 처리할 수 없습니다."),

    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}