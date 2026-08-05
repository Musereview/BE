package com.mr.global.apipayload.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.mr.global.apipayload.ApiResponse;
import com.mr.global.apipayload.code.CommonStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    @Test
    void unhandledException_logsRequestAndStackTraceWithoutSensitiveRequestData(CapturedOutput output) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/home");
        request.addHeader("Authorization", "Bearer sensitive-access-token");
        request.setQueryString("refreshToken=sensitive-refresh-token");
        RuntimeException exception = new RuntimeException("unexpected home failure");

        ResponseEntity<ApiResponse<Object>> response = handler.handleAllException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(ApiResponse.onFailure(
                CommonStatus.INTERNAL_SERVER_ERROR.getCode(),
                CommonStatus.INTERNAL_SERVER_ERROR.getMessage(),
                null
        ));
        assertThat(output).contains(
                "처리되지 않은 서버 예외: method=GET, uri=/api/home",
                "java.lang.RuntimeException: unexpected home failure"
        );
        assertThat(output).doesNotContain("sensitive-access-token", "sensitive-refresh-token", "Authorization");
    }
}
