package com.mr.global.apipayload.handler;

import com.mr.global.apipayload.ApiResponse;
import com.mr.global.apipayload.code.CommonStatus;
import com.mr.global.apipayload.exception.GeneralException;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    // @Valid 검증 실패 예외
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        var status = CommonStatus.INVALID_INPUT_VALUE;
        var errors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "올바르지 않은 형식입니다."
                ));
        var response = ApiResponse.onFailure(status.getCode(), status.getMessage(), errors);
        return new ResponseEntity<>(response, status.getStatus());
    }

    // JSON 파싱 에러
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        var status = CommonStatus.HTTP_MESSAGE_NOT_READABLE;
        var response = ApiResponse.onFailure(status.getCode(), status.getMessage(), null);
        return new ResponseEntity<>(response, status.getStatus());
    }

    // 파라미터 제약 조건 위반 예외
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(ConstraintViolationException e) {
        var status = CommonStatus.INVALID_INPUT_VALUE;
        var errors = e.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage()
                ));
        var response = ApiResponse.onFailure(status.getCode(), status.getMessage(), errors);
        return new ResponseEntity<>(response, status.getStatus());
    }

    // 그 외  전체 서버 에러 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllException(Exception e) {
        var status = CommonStatus.INTERNAL_SERVER_ERROR;
        var response = ApiResponse.onFailure(status.getCode(), status.getMessage(), null);
        return new ResponseEntity<>(response, status.getStatus());
    }
}