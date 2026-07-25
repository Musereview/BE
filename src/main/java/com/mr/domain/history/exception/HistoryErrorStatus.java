package com.mr.domain.history.exception;

import com.mr.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum HistoryErrorStatus implements BaseCode {

    HISTORY_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "HISTORY_400_01", "요청한 페이지 정보가 올바르지 않습니다."),

    HISTORY_INVALID_ID(HttpStatus.BAD_REQUEST, "HISTORY_400_02", "연주 히스토리 ID가 올바르지 않습니다."),

    HISTORY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "HISTORY_403_01", "연주 히스토리 조회 권한이 없습니다."),

    HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "HISTORY_404_01", "연주 히스토리를 찾을 수 없습니다."),
    
    HISTORY_NOT_COMPLETED(HttpStatus.CONFLICT, "HISTORY_409_01", "완료된 연주 히스토리만 조회할 수 있습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
