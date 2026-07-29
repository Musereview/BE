package com.mr.domain.home.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.home.dto.res.HomeResponseDTO;
import com.mr.domain.home.dto.res.HomeResponseDTO.PracticeSummary;
import com.mr.domain.home.dto.res.HomeResponseDTO.Streak;
import com.mr.domain.home.dto.res.HomeResponseDTO.UserSummary;
import com.mr.domain.home.service.HomeService;
import com.mr.domain.user.entity.enums.UserRole;
import com.mr.global.apipayload.handler.GlobalExceptionHandler;
import com.mr.global.security.principal.CustomUserDetails;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private HomeService homeService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
                .findModulesViaServiceLoader(true)
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(new HomeController(homeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.ROLE_STUDENT);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/home - 조회 성공")
    void getHome_success() throws Exception {
        HomeResponseDTO response = new HomeResponseDTO(
                new UserSummary(1L, "김뮤즈", "https://cdn.example.com/1.png", null, null),
                new Streak(0, "오늘부터 연습을 시작해보세요!", List.of()),
                new PracticeSummary(0, 0, "7월"),
                null,
                List.of(),
                List.of()
        );
        given(homeService.getHome(anyLong())).willReturn(response);

        mockMvc.perform(get("/api/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.user.nickname").value("김뮤즈"))
                .andExpect(jsonPath("$.data.currentLearning").isEmpty())
                .andExpect(jsonPath("$.data.recommendedLearnings").isEmpty());
    }
}
