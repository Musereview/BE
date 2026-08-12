package com.mr.domain.auth.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mr.domain.auth.dto.res.MasterAuthResponse;
import com.mr.domain.auth.service.MasterAuthService;
import com.mr.global.apipayload.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MasterAuthControllerTest {

    @Mock
    private MasterAuthService masterAuthService;

    @Test
    @DisplayName("요청값 없이 설정된 사용자 ID로 Access Token을 발급한다")
    void issueAccessToken_usesConfiguredUserId() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MasterAuthController(masterAuthService, 27L))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        given(masterAuthService.issueAccessToken(27L))
                .willReturn(new MasterAuthResponse(27L, "access-token", 1800L));

        mockMvc.perform(post("/api/auth/master-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(27L))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
        verify(masterAuthService).issueAccessToken(27L);
    }
}
