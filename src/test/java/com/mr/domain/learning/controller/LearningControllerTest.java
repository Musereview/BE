package com.mr.domain.learning.controller;

import com.mr.domain.learning.dto.res.LearningPracticeDataResponseDTO;
import com.mr.domain.learning.exception.LearningErrorStatus;
import com.mr.domain.learning.service.LearningService;
import com.mr.domain.user.entity.enums.UserRole;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.apipayload.handler.GlobalExceptionHandler;
import com.mr.global.security.principal.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LearningControllerTest {

    @Mock
    private LearningService learningService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new LearningController(learningService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.ROLE_STUDENT);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 연습_실행_정보_조회_성공() throws Exception {
        when(learningService.getPracticeData(anyLong(), anyLong()))
                .thenReturn(new LearningPracticeDataResponseDTO.PracticeDataResultDTO(
                        90, "C", "{ \"notes\": [] }"));

        mockMvc.perform(get("/api/learnings/1/steps/12/practice-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.bpm").value(90))
                .andExpect(jsonPath("$.data.keySignature").value("C"));
    }

    @Test
    void 연습_실행_정보_조회_실패_학습_없음() throws Exception {
        when(learningService.getPracticeData(anyLong(), anyLong()))
                .thenThrow(new GeneralException(LearningErrorStatus.LEARNING_NOT_FOUND));

        mockMvc.perform(get("/api/learnings/999/steps/12/practice-data"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEARNING_404_01"));
    }

    @Test
    void 연습_실행_정보_조회_실패_단계_불일치() throws Exception {
        when(learningService.getPracticeData(anyLong(), anyLong()))
                .thenThrow(new GeneralException(LearningErrorStatus.LEARNING_STEP_NOT_FOUND));

        mockMvc.perform(get("/api/learnings/1/steps/999/practice-data"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEARNING_404_02"));
    }

    @Test
    void 연습_실행_정보_조회_실패_실습_데이터_없음() throws Exception {
        when(learningService.getPracticeData(anyLong(), anyLong()))
                .thenThrow(new GeneralException(LearningErrorStatus.PLAYING_EXAMPLE_NOT_FOUND));

        mockMvc.perform(get("/api/learnings/1/steps/12/practice-data"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEARNING_404_03"));
    }
}
