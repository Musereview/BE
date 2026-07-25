package com.mr.domain.learning.controller;

import com.mr.domain.learning.dto.res.LearningCurriculumResponseDTO;
import com.mr.domain.learning.dto.res.LearningPracticeDataResponseDTO;
import com.mr.domain.learning.dto.res.LearningTheoryListResponseDTO;
import com.mr.domain.learning.exception.LearningErrorStatus;
import com.mr.domain.learning.service.LearningService;
import com.mr.domain.user.entity.enums.UserRole;
import com.mr.domain.user.exception.UserErrorStatus;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
    void 커리큘럼_조회_성공() throws Exception {
        LearningCurriculumResponseDTO.ProgressInfo progress =
                new LearningCurriculumResponseDTO.ProgressInfo(1, 4, 25);
        LearningCurriculumResponseDTO.StepItem step = new LearningCurriculumResponseDTO.StepItem(
                11L, 1, "9th 텐션 노트 활용하기", "설명", 10, "COMPLETED", 93);
        LearningCurriculumResponseDTO.CurriculumResultDTO response = new LearningCurriculumResponseDTO.CurriculumResultDTO(
                1L, "Tension Notes", "부제목", "ADVANCED", "이론 설명", "연습 팁", progress, List.of(step));

        when(learningService.getCurriculum(anyLong(), anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/learnings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learningId").value(1))
                .andExpect(jsonPath("$.data.progress.progressRate").value(25))
                .andExpect(jsonPath("$.data.steps[0].status").value("COMPLETED"));
    }

    @Test
    void 커리큘럼_조회_실패_학습_없음() throws Exception {
        when(learningService.getCurriculum(anyLong(), anyLong()))
                .thenThrow(new GeneralException(LearningErrorStatus.LEARNING_NOT_FOUND));

        mockMvc.perform(get("/api/learnings/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEARNING_404_01"));
    }

    @Test
    void 연습_실행_정보_조회_성공() throws Exception {
        when(learningService.getPracticeData(anyLong(), anyLong()))
                .thenReturn(new LearningPracticeDataResponseDTO.PracticeDataResultDTO(
                        90, "C", "{\"notes\":[60,64,67]}"));

        mockMvc.perform(get("/api/learnings/1/steps/12/practice-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.bpm").value(90))
                .andExpect(jsonPath("$.data.keySignature").value("C"))
                .andExpect(jsonPath("$.data.midiData.notes[0]").value(60));
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

    @Test
    void 학습_주제_목록_조회_성공() throws Exception {
        when(learningService.getTheoryList(anyLong(), anyString()))
                .thenReturn(new LearningTheoryListResponseDTO.TheoryListResultDTO(List.of(
                        new LearningTheoryListResponseDTO.TheoryItem(2L, "Diatonic Chords", "BEGINNER", "요약")
                )));

        mockMvc.perform(get("/api/learnings/theory").param("difficulty", "BEGINNER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].learningId").value(2))
                .andExpect(jsonPath("$.data.items[0].difficulty").value("BEGINNER"));
    }

    @Test
    void 학습_주제_목록_조회_실패_difficulty_누락() throws Exception {
        when(learningService.getTheoryList(anyLong(), org.mockito.ArgumentMatchers.isNull()))
                .thenThrow(new GeneralException(LearningErrorStatus.INVALID_DIFFICULTY));

        mockMvc.perform(get("/api/learnings/theory"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LEARNING_400_02"));
    }

    @Test
    void 학습_주제_목록_조회_실패_유저_없음() throws Exception {
        when(learningService.getTheoryList(anyLong(), anyString()))
                .thenThrow(new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        mockMvc.perform(get("/api/learnings/theory").param("difficulty", "BEGINNER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_404_01"));
    }
}
