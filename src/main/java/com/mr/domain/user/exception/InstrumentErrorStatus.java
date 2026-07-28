package com.mr.domain.user.exception;

import com.mr.global.apipayload.code.BaseCode;
import org.springframework.http.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InstrumentErrorStatus implements BaseCode {

    CODE_REQUIRED(HttpStatus.BAD_REQUEST, "INSTRUMENT_400_01", "code는 필수입니다."),
    NAME_REQUIRED(HttpStatus.BAD_REQUEST, "INSTRUMENT_400_02", "name은 필수입니다."),
    INSTRUMENT_NOT_SEEDED(HttpStatus.INTERNAL_SERVER_ERROR, "INSTRUMENT_500_01", "PIANO 악기 시드 데이터가 존재하지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
