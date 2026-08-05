package com.mr.global.apipayload.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerMvcTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void unhandledException_flowsThroughControllerAdvice(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/test/unhandled")
                        .header("Authorization", "Bearer sensitive-access-token")
                        .queryParam("refreshToken", "sensitive-refresh-token"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_500_01"))
                .andExpect(jsonPath("$.message").value("서버 에러가 발생했습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertThat(output).contains(
                "처리되지 않은 서버 예외: method=GET, uri=/test/unhandled",
                "java.lang.IllegalStateException: mvc flow failure"
        );
        assertThat(output).doesNotContain("sensitive-access-token", "sensitive-refresh-token", "Authorization");
    }

    @RestController
    @RequestMapping("/test")
    static class FailingController {

        @GetMapping("/unhandled")
        String throwUnhandledException() {
            throw new IllegalStateException("mvc flow failure");
        }
    }
}
