package com.mr.global.apipayload.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mr.global.apipayload.ApiResponse;
import com.mr.global.apipayload.code.CommonStatus;
import com.mr.global.apipayload.exception.GeneralException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class GlobalExceptionHandlerContractTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void generalException_preservesDomainErrorResponse() {
        ResponseEntity<ApiResponse<Object>> response =
                handler.handleGeneralException(new GeneralException(CommonStatus.FORBIDDEN));

        assertFailure(response, CommonStatus.FORBIDDEN, null);
    }

    @Test
    void methodArgumentNotValidException_preservesValidationErrorResponse() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("request", "nickname", "닉네임은 필수입니다.")
        ));
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiResponse<Object>> response = handler.handleMethodArgumentNotValidException(exception);

        assertFailure(
                response,
                CommonStatus.INVALID_INPUT_VALUE,
                Map.of("nickname", "닉네임은 필수입니다.")
        );
    }

    @Test
    void httpMessageNotReadableException_preservesParsingErrorResponse() {
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);

        ResponseEntity<ApiResponse<Object>> response = handler.handleHttpMessageNotReadableException(exception);

        assertFailure(response, CommonStatus.HTTP_MESSAGE_NOT_READABLE, null);
    }

    @Test
    @SuppressWarnings("unchecked")
    void constraintViolationException_preservesValidationErrorResponse() {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path propertyPath = mock(Path.class);
        when(propertyPath.toString()).thenReturn("request.page");
        when(violation.getPropertyPath()).thenReturn(propertyPath);
        when(violation.getMessage()).thenReturn("페이지는 0 이상이어야 합니다.");
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ApiResponse<Object>> response = handler.handleConstraintViolationException(exception);

        assertFailure(
                response,
                CommonStatus.INVALID_INPUT_VALUE,
                Map.of("request.page", "페이지는 0 이상이어야 합니다.")
        );
    }

    @Test
    void methodArgumentTypeMismatchException_preservesTypeMismatchErrorResponse() {
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        when(exception.getName()).thenReturn("period");

        ResponseEntity<ApiResponse<Object>> response = handler.handleMethodArgumentTypeMismatchException(exception);

        assertFailure(
                response,
                CommonStatus.INVALID_INPUT_VALUE,
                Map.of("period", "요청 파라미터 형식이 올바르지 않습니다.")
        );
    }

    private void assertFailure(
            ResponseEntity<ApiResponse<Object>> response,
            CommonStatus status,
            Object data
    ) {
        assertThat(response.getStatusCode()).isEqualTo(status.getStatus());
        assertThat(response.getBody()).isEqualTo(ApiResponse.onFailure(
                status.getCode(),
                status.getMessage(),
                data
        ));
    }
}
