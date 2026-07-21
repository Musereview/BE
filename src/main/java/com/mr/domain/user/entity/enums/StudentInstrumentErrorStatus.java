package com.mr.domain.user.entity.enums;

import com.mr.global.apipayload.code.BaseCode;
import org.springframework.http.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StudentInstrumentErrorStatus implements BaseCode {

    STUDENT_REQUIRED(HttpStatus.BAD_REQUEST, "STUDENT_INSTRUMENT_400_01", "student는 필수입니다."),
    INSTRUMENT_REQUIRED(HttpStatus.BAD_REQUEST, "STUDENT_INSTRUMENT_400_02", "instrument는 필수입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
