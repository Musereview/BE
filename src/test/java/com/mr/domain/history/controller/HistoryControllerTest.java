package com.mr.domain.history.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.history.dto.res.HistoryDetailResponseDTO;
import com.mr.domain.history.dto.res.HistoryListResponseDTO;
import com.mr.domain.history.exception.HistoryErrorStatus;
import com.mr.domain.history.service.HistoryService;
import com.mr.domain.user.entity.enums.UserRole;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.apipayload.handler.GlobalExceptionHandler;
import com.mr.global.security.principal.CustomUserDetails;

import java.time.Instant;
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
class HistoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private HistoryService historyService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
                .findModulesViaServiceLoader(true)
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(new HistoryController(historyService))
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
    @DisplayName("GET /api/histories - 목록 조회 성공")
    void getHistories_success() throws Exception {
        HistoryListResponseDTO.Item item = new HistoryListResponseDTO.Item(
                1L, 11L, null, "제목", null, null, 5, 300, null, "오늘"
        );
        HistoryListResponseDTO response = HistoryListResponseDTO.of(0, 10, false, List.of(item));
        given(historyService.getHistories(anyLong(), anyInt(), anyInt(), any())).willReturn(response);

        mockMvc.perform(get("/api/histories").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.items[0].backingTrackId").value(11L));
    }

    @Test
    @DisplayName("GET /api/histories - size가 51이면 400")
    void getHistories_sizeTooLarge_returns400() throws Exception {
        given(historyService.getHistories(anyLong(), anyInt(), anyInt(), any()))
                .willThrow(new GeneralException(HistoryErrorStatus.HISTORY_INVALID_REQUEST));

        mockMvc.perform(get("/api/histories").param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("HISTORY_400_01"));
    }

    @Test
    @DisplayName("GET /api/histories - period에 정의되지 않은 값이 오면 400(500 아님)")
    void getHistories_invalidPeriodEnum_returns400() throws Exception {
        mockMvc.perform(get("/api/histories").param("period", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_01"));
    }

    @Test
    @DisplayName("GET /api/histories/{id} - playingId가 숫자가 아니면 400(500 아님)")
    void getHistoryDetail_nonNumericPlayingId_returns400() throws Exception {
        mockMvc.perform(get("/api/histories/{playingId}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_01"));
    }

    @Test
    @DisplayName("GET /api/histories/{id} - playingId가 1 미만이면 400")
    void getHistoryDetail_invalidPlayingId_returns400() throws Exception {
        willThrow(new GeneralException(HistoryErrorStatus.HISTORY_INVALID_ID))
                .given(historyService).getHistoryDetail(anyLong(), anyLong());

        mockMvc.perform(get("/api/histories/{playingId}", 0L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("HISTORY_400_02"));
    }

    @Test
    @DisplayName("GET /api/histories/{id} - 상세 조회 성공")
    void getHistoryDetail_success() throws Exception {
        HistoryDetailResponseDTO response = new HistoryDetailResponseDTO(
                1L, 11L, "제목", "장르", "C Major", 120, "4/4",
                Instant.parse("2026-07-24T10:00:00Z"), 5, 300,
                "https://example.com/recording.webm", "https://example.com/backing-track.mp3",
                List.of(), null, null, List.of()
        );
        given(historyService.getHistoryDetail(anyLong(), anyLong())).willReturn(response);

        mockMvc.perform(get("/api/histories/{playingId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.playingId").value(1L))
                .andExpect(jsonPath("$.data.backingTrackId").value(11L))
                .andExpect(jsonPath("$.data.title").value("제목"))
                .andExpect(jsonPath("$.data.recordingFileUrl").value("https://example.com/recording.webm"))
                .andExpect(jsonPath("$.data.backingTrackAudioFileUrl")
                        .value("https://example.com/backing-track.mp3"));
    }

    @Test
    @DisplayName("GET /api/histories/{id} - 다른 사용자의 기록이면 403")
    void getHistoryDetail_accessDenied_returns403() throws Exception {
        willThrow(new GeneralException(HistoryErrorStatus.HISTORY_ACCESS_DENIED))
                .given(historyService).getHistoryDetail(anyLong(), anyLong());

        mockMvc.perform(get("/api/histories/{playingId}", 1L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("HISTORY_403_01"));
    }

    @Test
    @DisplayName("GET /api/histories/{id} - 존재하지 않으면 404")
    void getHistoryDetail_notFound_returns404() throws Exception {
        willThrow(new GeneralException(HistoryErrorStatus.HISTORY_NOT_FOUND))
                .given(historyService).getHistoryDetail(anyLong(), anyLong());

        mockMvc.perform(get("/api/histories/{playingId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HISTORY_404_01"));
    }

    @Test
    @DisplayName("GET /api/histories/{id} - COMPLETED 아니면 409")
    void getHistoryDetail_notCompleted_returns409() throws Exception {
        willThrow(new GeneralException(HistoryErrorStatus.HISTORY_NOT_COMPLETED))
                .given(historyService).getHistoryDetail(anyLong(), anyLong());

        mockMvc.perform(get("/api/histories/{playingId}", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("HISTORY_409_01"));
    }
}
