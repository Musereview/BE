package com.mr.domain.analysis.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.analysis.dto.res.AnalysisResultResponseDTO;
import com.mr.domain.analysis.dto.res.AnalysisStatusResponseDTO;
import com.mr.domain.analysis.entity.enums.AnalysisGrade;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.entity.enums.ContentFormat;
import com.mr.domain.analysis.entity.enums.LlmStatus;
import com.mr.domain.analysis.entity.enums.ReportGenerationType;
import com.mr.domain.analysis.exception.AnalysisErrorStatus;
import com.mr.domain.analysis.service.AnalysisService;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.apipayload.handler.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AnalysisControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AnalysisService analysisService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
                .findModulesViaServiceLoader(true)
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(new AnalysisController(analysisService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("GET /api/analyses/{id}/status - 상태 조회 성공")
    void getAnalysisStatus_success() throws Exception {
        AnalysisStatusResponseDTO response = new AnalysisStatusResponseDTO(
                1L,
                AnalysisStatus.PROCESSING,
                null,
                "연습 결과를 분석 중입니다.",
                LocalDateTime.of(2026, 7, 24, 10, 0),
                null
        );
        given(analysisService.getAnalysisStatus(anyLong(), anyLong())).willReturn(response);

        mockMvc.perform(get("/api/analyses/{analysisId}/status", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.analysisId").value(1L))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"))
                .andExpect(jsonPath("$.data.message").value("연습 결과를 분석 중입니다."));
    }

    @Test
    @DisplayName("GET /api/analyses/{id}/status - 존재하지 않는 분석이면 404")
    void getAnalysisStatus_notFound() throws Exception {
        given(analysisService.getAnalysisStatus(anyLong(), anyLong()))
                .willThrow(new GeneralException(AnalysisErrorStatus.ANALYSIS_NOT_FOUND));

        mockMvc.perform(get("/api/analyses/{analysisId}/status", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("ANALYSIS_404_01"));
    }

    @Test
    @DisplayName("GET /api/analyses/{id}/status - 다른 사용자의 분석이면 403")
    void getAnalysisStatus_accessDenied() throws Exception {
        willThrow(new GeneralException(AnalysisErrorStatus.ANALYSIS_ACCESS_DENIED))
                .given(analysisService).getAnalysisStatus(anyLong(), anyLong());

        mockMvc.perform(get("/api/analyses/{analysisId}/status", 1L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ANALYSIS_403_01"));
    }

    @Test
    @DisplayName("GET /api/analyses/{id} - 결과 조회 성공")
    void getAnalysisResult_success() throws Exception {
        AnalysisResultResponseDTO response = new AnalysisResultResponseDTO(
                1L,
                1L,
                1,
                8,
                85,
                AnalysisGrade.GOOD,
                "테스트 요약",
                new AnalysisResultResponseDTO.DomainScores(
                        new BigDecimal("80.00"),
                        new BigDecimal("75.50"),
                        new BigDecimal("90.00"),
                        new BigDecimal("70.00")
                ),
                new AnalysisResultResponseDTO.Report(
                        10L,
                        ReportGenerationType.LLM,
                        LlmStatus.SUCCESS,
                        ContentFormat.MARKDOWN,
                        "리포트 본문",
                        "gpt-4",
                        "v1",
                        LocalDateTime.of(2026, 7, 24, 10, 0),
                        LocalDateTime.of(2026, 7, 24, 10, 0)
                ),
                null,
                LocalDateTime.of(2026, 7, 24, 9, 0),
                LocalDateTime.of(2026, 7, 24, 10, 0)
        );
        given(analysisService.getAnalysisResult(anyLong(), anyLong())).willReturn(response);

        mockMvc.perform(get("/api/analyses/{analysisId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.analysisId").value(1L))
                .andExpect(jsonPath("$.data.totalScore").value(85))
                .andExpect(jsonPath("$.data.grade").value("GOOD"))
                .andExpect(jsonPath("$.data.domainScores.scaleScore").value(80.00))
                .andExpect(jsonPath("$.data.report.modelName").value("gpt-4"));
    }

    @Test
    @DisplayName("GET /api/analyses/{id} - 완료되지 않은 분석이면 409")
    void getAnalysisResult_notCompleted() throws Exception {
        given(analysisService.getAnalysisResult(anyLong(), anyLong()))
                .willThrow(new GeneralException(AnalysisErrorStatus.ANALYSIS_NOT_COMPLETED));

        mockMvc.perform(get("/api/analyses/{analysisId}", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ANALYSIS_409_01"));
    }
}
