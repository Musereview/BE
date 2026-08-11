package com.mr.domain.learning.controller;

import com.mr.domain.learning.dto.res.LearningAccompanimentListResponseDTO;
import com.mr.domain.learning.dto.res.LearningCurriculumResponseDTO;
import com.mr.domain.learning.dto.res.LearningHomeResponseDTO;
import com.mr.domain.learning.dto.res.LearningPracticeDataResponseDTO;
import com.mr.domain.learning.dto.res.LearningStepDetailResponseDTO;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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
    @DisplayName("GET /api/learnings/{learningId} - 커리큘럼 조회 성공")
    void getCurriculum_success() throws Exception {
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
    @DisplayName("GET /api/learnings/{learningId} - 커리큘럼 조회 실패(학습 없음)")
    void getCurriculum_learningNotFound_throws404() throws Exception {
        when(learningService.getCurriculum(anyLong(), anyLong()))
                .thenThrow(new GeneralException(LearningErrorStatus.LEARNING_NOT_FOUND));

        mockMvc.perform(get("/api/learnings/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEARNING_404_01"));
    }

    @Test
    @DisplayName("GET /api/learnings/{learningId}/steps/{stepId} - 단계별 조회 성공")
    void getStepDetail_success() throws Exception {
        LearningStepDetailResponseDTO.ModelPerformance modelPerformance =
                new LearningStepDetailResponseDTO.ModelPerformance("제목", "설명", "https://audio.url", 154);
        LearningStepDetailResponseDTO.ChordExampleItem chordExample =
                new LearningStepDetailResponseDTO.ChordExampleItem("Cmaj7", "설명", List.of(60, 64, 67));
        LearningStepDetailResponseDTO.StepDetailResultDTO response = new LearningStepDetailResponseDTO.StepDetailResultDTO(
                1L, 12L, "Tension Notes", "ADVANCED", 2, "11th 텐션 노트 활용하기",
                "이론", "팁", modelPerformance, List.of(chordExample));

        when(learningService.getStepDetail(anyLong(), anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/learnings/1/steps/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stepTitle").value("11th 텐션 노트 활용하기"))
                .andExpect(jsonPath("$.data.modelPerformance.durationSeconds").value(154))
                .andExpect(jsonPath("$.data.chordExamples[0].chordName").value("Cmaj7"));
    }

    @Test
    @DisplayName("GET /api/learnings/{learningId}/steps/{stepId} - 단계별 조회 실패(학습 없음)")
    void getStepDetail_learningNotFound_throws404() throws Exception {
        when(learningService.getStepDetail(anyLong(), anyLong()))
                .thenThrow(new GeneralException(LearningErrorStatus.LEARNING_NOT_FOUND));

        mockMvc.perform(get("/api/learnings/999/steps/12"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEARNING_404_01"));
    }

    @Test
    @DisplayName("GET /api/learnings/{learningId}/steps/{stepId} - 단계별 조회 실패(단계 불일치)")
    void getStepDetail_stepMismatch_throws404() throws Exception {
        when(learningService.getStepDetail(anyLong(), anyLong()))
                .thenThrow(new GeneralException(LearningErrorStatus.LEARNING_STEP_NOT_FOUND));

        mockMvc.perform(get("/api/learnings/1/steps/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEARNING_404_02"));
    }

    @Test
    @DisplayName("GET /api/learnings/{learningId}/steps/{stepId}/practice-data - 연습 실행 정보 조회 성공")
    void getPracticeData_success() throws Exception {
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
    @DisplayName("GET /api/learnings/{learningId}/steps/{stepId}/practice-data - 연습 실행 정보 조회 실패(학습 없음)")
    void getPracticeData_learningNotFound_throws404() throws Exception {
        when(learningService.getPracticeData(anyLong(), anyLong()))
                .thenThrow(new GeneralException(LearningErrorStatus.LEARNING_NOT_FOUND));

        mockMvc.perform(get("/api/learnings/999/steps/12/practice-data"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEARNING_404_01"));
    }

    @Test
    @DisplayName("GET /api/learnings/{learningId}/steps/{stepId}/practice-data - 연습 실행 정보 조회 실패(단계 불일치)")
    void getPracticeData_stepMismatch_throws404() throws Exception {
        when(learningService.getPracticeData(anyLong(), anyLong()))
                .thenThrow(new GeneralException(LearningErrorStatus.LEARNING_STEP_NOT_FOUND));

        mockMvc.perform(get("/api/learnings/1/steps/999/practice-data"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEARNING_404_02"));
    }

    @Test
    @DisplayName("GET /api/learnings/{learningId}/steps/{stepId}/practice-data - 연습 실행 정보 조회 실패(실습 데이터 없음)")
    void getPracticeData_practiceDataNotFound_throws404() throws Exception {
        when(learningService.getPracticeData(anyLong(), anyLong()))
                .thenThrow(new GeneralException(LearningErrorStatus.PLAYING_EXAMPLE_NOT_FOUND));

        mockMvc.perform(get("/api/learnings/1/steps/12/practice-data"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEARNING_404_03"));
    }

    @Test
    @DisplayName("GET /api/learnings/theory - 학습 주제 목록 조회 성공")
    void getTheoryList_success() throws Exception {
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
    @DisplayName("GET /api/learnings/theory - 학습 주제 목록 조회 실패(difficulty 누락)")
    void getTheoryList_missingDifficulty_throws400() throws Exception {
        when(learningService.getTheoryList(anyLong(), org.mockito.ArgumentMatchers.isNull()))
                .thenThrow(new GeneralException(LearningErrorStatus.INVALID_DIFFICULTY));

        mockMvc.perform(get("/api/learnings/theory"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LEARNING_400_02"));
    }

    @Test
    @DisplayName("GET /api/learnings/theory - 학습 주제 목록 조회 실패(유저 없음)")
    void getTheoryList_userNotFound_throws404() throws Exception {
        when(learningService.getTheoryList(anyLong(), anyString()))
                .thenThrow(new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        mockMvc.perform(get("/api/learnings/theory").param("difficulty", "BEGINNER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_404_01"));
    }

    @Test
    @DisplayName("GET /api/learnings/accompaniment - 반주법 목록 조회 성공")
    void getAccompanimentList_success() throws Exception {
        LearningAccompanimentListResponseDTO.AccompanimentItem item =
                new LearningAccompanimentListResponseDTO.AccompanimentItem(5L, "Chapter 1", "설명", 10, 100);
        when(learningService.getAccompanimentList(anyLong()))
                .thenReturn(LearningAccompanimentListResponseDTO.AccompanimentListResultDTO.of(List.of(item)));

        mockMvc.perform(get("/api/learnings/accompaniment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.items[0].progressRate").value(100));
    }

    @Test
    @DisplayName("GET /api/learnings/accompaniment - 반주법 목록 조회 실패(유저 없음)")
    void getAccompanimentList_userNotFound_throws404() throws Exception {
        when(learningService.getAccompanimentList(anyLong()))
                .thenThrow(new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        mockMvc.perform(get("/api/learnings/accompaniment"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_404_01"));
    }

    @Test
    @DisplayName("GET /api/learnings/home - 학습 홈 조회 성공")
    void getHome_success() throws Exception {
        LearningHomeResponseDTO.CurrentLearning currentLearning =
                new LearningHomeResponseDTO.CurrentLearning(1L, "Tension Notes", "ADVANCED", "11th 텐션 노트 활용하기", "RETRY", 10, 13L);
        LearningHomeResponseDTO.TheoryPackageItem theoryItem =
                new LearningHomeResponseDTO.TheoryPackageItem(2L, "Diatonic Chords", "BEGINNER", "요약");
        LearningAccompanimentListResponseDTO.AccompanimentItem accompanimentItem =
                new LearningAccompanimentListResponseDTO.AccompanimentItem(5L, "Chapter 1", "설명", 10, 100);

        when(learningService.getHome(anyLong())).thenReturn(LearningHomeResponseDTO.HomeResultDTO.of(
                currentLearning, List.of(theoryItem), List.of(accompanimentItem)));

        mockMvc.perform(get("/api/learnings/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentLearning.stepTitle").value("11th 텐션 노트 활용하기"))
                .andExpect(jsonPath("$.data.currentLearning.status").value("RETRY"))
                .andExpect(jsonPath("$.data.currentLearning.nextStepId").value(13))
                .andExpect(jsonPath("$.data.theoryPackages[0].title").value("Diatonic Chords"))
                .andExpect(jsonPath("$.data.accompanimentPackages[0].progressRate").value(100));
    }

    @Test
    @DisplayName("GET /api/learnings/home - 학습 홈 조회 성공(최근 학습 없으면 currentLearning null)")
    void getHome_noRecentLearning_currentLearningIsNull() throws Exception {
        when(learningService.getHome(anyLong())).thenReturn(
                LearningHomeResponseDTO.HomeResultDTO.of(null, List.of(), List.of()));

        mockMvc.perform(get("/api/learnings/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentLearning").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/learnings/home - 학습 홈 조회 실패(유저 없음)")
    void getHome_userNotFound_throws404() throws Exception {
        when(learningService.getHome(anyLong()))
                .thenThrow(new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        mockMvc.perform(get("/api/learnings/home"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_404_01"));
    }
}
