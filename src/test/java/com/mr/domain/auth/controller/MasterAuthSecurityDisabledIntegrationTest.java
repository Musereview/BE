package com.mr.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.mr.domain.auth.service.MasterAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class MasterAuthSecurityDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MasterAuthService masterAuthService;

    @Test
    @DisplayName("마스터 인증 설정이 없으면 인증 없는 발급 요청을 차단한다")
    void issueAccessToken_missingProperty_blocksAnonymousRequest() throws Exception {
        mockMvc.perform(post("/api/auth/master-token"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }
}
