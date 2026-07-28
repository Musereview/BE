package com.mr.domain.statistics.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.statistics.dto.res.StatisticsResponseDTO;
import com.mr.domain.statistics.dto.res.StatisticsResponseDTO.DomainGrowth;
import com.mr.domain.statistics.dto.res.StatisticsResponseDTO.TrendItem;
import com.mr.domain.statistics.dto.res.StatisticsResponseDTO.WeeklySummary;
import com.mr.domain.statistics.dto.res.StatisticsResponseDTO.WeeklyTrend;
import com.mr.domain.statistics.entity.enums.SkillType;
import com.mr.domain.statistics.exception.StatisticsErrorStatus;
import com.mr.domain.statistics.service.StatisticsService;
import com.mr.domain.user.entity.enums.UserRole;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.apipayload.handler.GlobalExceptionHandler;
import com.mr.global.security.principal.CustomUserDetails;
import java.math.BigDecimal;
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
class StatisticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StatisticsService statisticsService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
                .findModulesViaServiceLoader(true)
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(new StatisticsController(statisticsService))
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

    private StatisticsResponseDTO sampleResponse() {
        return new StatisticsResponseDTO(
                new WeeklySummary(BigDecimal.valueOf(91.0), 65, 6, BigDecimal.valueOf(4.0), 18, 2),
                List.of(new DomainGrowth(SkillType.SCALE, "스케일",
                        BigDecimal.valueOf(85.0), BigDecimal.valueOf(77.0), BigDecimal.valueOf(8.0))),
                new WeeklyTrend(9, List.of(new TrendItem("이번주", BigDecimal.valueOf(93.0))))
        );
    }

    @Test
    @DisplayName("GET /api/users/me/statistics - 조회 성공")
    void getStatistics_success() throws Exception {
        given(statisticsService.getStatistics(anyLong(), any(), any(), any())).willReturn(sampleResponse());

        mockMvc.perform(get("/api/users/me/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.weeklySummary.accuracy").value(91.0))
                .andExpect(jsonPath("$.data.domainGrowth[0].domain").value("SCALE"))
                .andExpect(jsonPath("$.data.weeklyTrend.diffFromPreviousWeek").value(9));
    }

    @Test
    @DisplayName("GET /api/users/me/statistics - period와 from 동시 전달 시 400")
    void getStatistics_periodAndFromTogether_returns400() throws Exception {
        willThrow(new GeneralException(StatisticsErrorStatus.STATISTICS_PERIOD_CONFLICT))
                .given(statisticsService).getStatistics(anyLong(), any(), any(), any());

        mockMvc.perform(get("/api/users/me/statistics").param("period", "WEEKLY").param("from", "2026-07-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STATISTICS_400_01"));
    }

    @Test
    @DisplayName("GET /api/users/me/statistics - from만 있고 to가 없으면 400")
    void getStatistics_fromWithoutTo_returns400() throws Exception {
        willThrow(new GeneralException(StatisticsErrorStatus.STATISTICS_INVALID_RANGE))
                .given(statisticsService).getStatistics(anyLong(), any(), any(), any());

        mockMvc.perform(get("/api/users/me/statistics").param("from", "2026-07-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STATISTICS_400_02"));
    }

    @Test
    @DisplayName("GET /api/users/me/statistics - period에 정의되지 않은 값이 오면 400(500 아님)")
    void getStatistics_invalidPeriodEnum_returns400() throws Exception {
        mockMvc.perform(get("/api/users/me/statistics").param("period", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_01"));
    }

    @Test
    @DisplayName("GET /api/users/me/statistics - 존재하지 않는 사용자면 404")
    void getStatistics_userNotFound_returns404() throws Exception {
        willThrow(new GeneralException(UserErrorStatus.USER_NOT_FOUND))
                .given(statisticsService).getStatistics(anyLong(), any(), any(), any());

        mockMvc.perform(get("/api/users/me/statistics"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_404_01"));
    }
}
