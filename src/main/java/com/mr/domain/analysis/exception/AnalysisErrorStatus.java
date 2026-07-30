package com.mr.domain.analysis.exception;

import com.mr.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AnalysisErrorStatus implements BaseCode {

    INVALID_BAR_ORDER(HttpStatus.BAD_REQUEST, "ANALYSIS_400_01", "\uBD84\uC11D \uC2DC\uC791 \uB9C8\uB514\uB294 \uC885\uB8CC \uB9C8\uB514\uBCF4\uB2E4 \uD074 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."),

    INVALID_BAR_RANGE(HttpStatus.BAD_REQUEST, "ANALYSIS_400_02", "\uBD84\uC11D \uAC00\uB2A5\uD55C \uB9C8\uB514 \uBC94\uC704\uB97C \uBC97\uC5B4\uB0AC\uC2B5\uB2C8\uB2E4."),

    ANALYSIS_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "ANALYSIS_400_03", "필수 정보가 누락되었습니다."),

    ANALYSIS_OWNER_MISMATCH(HttpStatus.BAD_REQUEST, "ANALYSIS_400_04", "playing 소유자와 user가 일치하지 않습니다."),

    ANALYSIS_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ANALYSIS_403_01", "해당 분석 결과에 접근할 수 없습니다."),

    ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "ANALYSIS_404_01", "분석 결과를 찾을 수 없습니다."),

    ANALYSIS_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "ANALYSIS_409_01", "\uC774\uBBF8 \uC9C4\uD589 \uC911\uC778 \uBD84\uC11D \uC694\uCCAD\uC774 \uC788\uC2B5\uB2C8\uB2E4."),

    ANALYSIS_NOT_COMPLETED(HttpStatus.CONFLICT, "ANALYSIS_409_02", "아직 완료되지 않은 분석입니다."),

    INVALID_RAW_RESULT(HttpStatus.INTERNAL_SERVER_ERROR, "ANALYSIS_500_01", "저장된 분석 결과를 처리할 수 없습니다."),

    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}