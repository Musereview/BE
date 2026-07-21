package com.mr.domain.user.entity.enums;

import com.mr.global.apipayload.code.BaseCode;
import org.springframework.http.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InstrumentErrorStatus implements BaseCode {

    CODE_REQUIRED(HttpStatus.BAD_REQUEST, "INSTRUMENT_400_01", "code는 필수입니다."),
    NAME_REQUIRED(HttpStatus.BAD_REQUEST, "INSTRUMENT_400_02", "name은 필수입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
