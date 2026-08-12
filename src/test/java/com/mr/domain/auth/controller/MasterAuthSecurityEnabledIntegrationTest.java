package com.mr.domain.auth.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mr.domain.auth.dto.res.MasterAuthResponse;
import com.mr.domain.auth.service.MasterAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "master-auth.enabled=true",
        "master-auth.user-id=27"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class MasterAuthSecurityEnabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MasterAuthService masterAuthService;

    @Test
    @DisplayName("마스터 인증이 활성화되면 인증 없이 설정된 사용자의 Access Token을 발급한다")
    void issueAccessToken_enabled_allowsAnonymousRequest() throws Exception {
        given(masterAuthService.issueAccessToken(27L))
                .willReturn(new MasterAuthResponse(27L, "access-token", 1800L));

        mockMvc.perform(post("/api/auth/master-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(27L))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.accessTokenExpiresInSeconds").value(1800L));
    }
}
