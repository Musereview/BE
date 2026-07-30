package com.mr.domain.statistics.exception;

import com.mr.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum StatisticsErrorStatus implements BaseCode {

    STATISTICS_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "STATISTICS_400_03", "필수 정보가 누락되었습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
