package com.mr.global.apiPayLoad.handler;

import com.mr.global.apiPayLoad.ApiResponse;
import com.mr.global.apiPayLoad.code.CommonStatus;
import com.mr.global.apiPayLoad.exception.GeneralException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 커스텀 예외
    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(GeneralException e) {
        var reason = e.getErrorReason();
        var response = ApiResponse.onFailure(reason.code(), reason.message(), null);
        return new ResponseEntity<>(response, reason.status());
    }

    // 그 외  전체 서버 에러 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllException(Exception e) {
        var status = CommonStatus.INTERNAL_SERVER_ERROR;
        var response = ApiResponse.onFailure(status.getCode(), status.getMessage(), null);
        return new ResponseEntity<>(response, status.getStatus());
    }
}