package com.mr.domain.analysis.exception;

import com.mr.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AnalysisErrorStatus implements BaseCode {

    INVALID_BAR_ORDER(HttpStatus.BAD_REQUEST, "ANALYSIS_400_01", "분석 시작 마디는 종료 마디보다 클 수 없습니다."),

    INVALID_BAR_RANGE(HttpStatus.BAD_REQUEST, "ANALYSIS_400_02", "분석 가능한 마디 범위를 벗어났습니다."),

    ANALYSIS_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "ANALYSIS_400_03", "필수 정보가 누락되었습니다."),

    ANALYSIS_OWNER_MISMATCH(HttpStatus.BAD_REQUEST, "ANALYSIS_400_04", "playing 소유자와 user가 일치하지 않습니다."),

    ANALYSIS_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ANALYSIS_403_01", "해당 분석 결과에 접근할 수 없습니다."),

    ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "ANALYSIS_404_01", "분석 결과를 찾을 수 없습니다."),

    ANALYSIS_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "ANALYSIS_409_01", "이미 진행 중인 분석 요청이 있습니다."),

    ANALYSIS_NOT_COMPLETED(HttpStatus.CONFLICT, "ANALYSIS_409_02", "아직 완료되지 않은 분석입니다."),

    INVALID_RAW_RESULT(HttpStatus.INTERNAL_SERVER_ERROR, "ANALYSIS_500_01", "저장된 분석 결과를 처리할 수 없습니다."),

    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
