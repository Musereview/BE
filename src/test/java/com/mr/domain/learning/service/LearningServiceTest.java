package com.mr.domain.learning.service;

import com.mr.domain.learning.dto.res.LearningCurriculumResponseDTO;
import com.mr.domain.learning.dto.res.LearningTheoryListResponseDTO;
import com.mr.domain.learning.entity.Learning;
import com.mr.domain.learning.entity.LearningStep;
import com.mr.domain.learning.entity.UserLearningProgress;
import com.mr.domain.learning.entity.enums.LearningCategory;
import com.mr.domain.learning.entity.enums.LearningDifficulty;
import com.mr.domain.learning.exception.LearningErrorStatus;
import com.mr.domain.learning.repository.LearningRepository;
import com.mr.domain.learning.repository.LearningStepRepository;
import com.mr.domain.learning.repository.PlayingExampleRepository;
import com.mr.domain.learning.repository.UserLearningProgressRepository;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningServiceTest {

    @Mock
    private UserLearningProgressRepository userLearningProgressRepository;
    @Mock
    private LearningStepRepository learningStepRepository;
    @Mock
    private LearningRepository learningRepository;
    @Mock
    private PlayingExampleRepository playingExampleRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LearningService learningService;

    @Test
    void 소문자와_공백이_포함된_difficulty도_정상_변환() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(learningRepository.findByCategoryAndDifficultyAndIsActiveTrueOrderByTitleAsc(
                LearningCategory.THEORY, LearningDifficulty.BEGINNER))
                .thenReturn(Collections.emptyList());

        LearningTheoryListResponseDTO.TheoryListResultDTO result =
                learningService.getTheoryList(1L, " beginner ");

        assertThat(result.items()).isEmpty();
        verify(learningRepository).findByCategoryAndDifficultyAndIsActiveTrueOrderByTitleAsc(
                LearningCategory.THEORY, LearningDifficulty.BEGINNER);
    }

    @Test
    void 존재하지_않는_difficulty값은_400() {
        assertThatThrownBy(() -> learningService.getTheoryList(1L, "invalid"))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode())
                        .isEqualTo(LearningErrorStatus.INVALID_DIFFICULTY));
    }

    @Test
    void 커리큘럼_조회_성공_진행률과_단계별_상태_계산() {
        Long userId = 1L;
        Long learningId = 10L;

        Learning learning = mock(Learning.class);
        when(learning.getId()).thenReturn(learningId);
        when(learning.getTitle()).thenReturn("Tension Notes");
        when(learning.getSummary()).thenReturn("부제목");
        when(learning.getDifficulty()).thenReturn(LearningDifficulty.ADVANCED);
        when(learning.getContent()).thenReturn("이론 설명");
        when(learning.getPracticeTip()).thenReturn("연습 팁");

        LearningStep step1 = mock(LearningStep.class);
        when(step1.getId()).thenReturn(11L);
        when(step1.getStepNo()).thenReturn(1);
        when(step1.getTitle()).thenReturn("9th 텐션 노트 활용하기");
        when(step1.getSummary()).thenReturn("설명1");
        when(step1.getEstimatedMinutes()).thenReturn(10);

        LearningStep step2 = mock(LearningStep.class);
        when(step2.getId()).thenReturn(12L);
        when(step2.getStepNo()).thenReturn(2);
        when(step2.getTitle()).thenReturn("11th 텐션 노트 활용하기");
        when(step2.getSummary()).thenReturn("설명2");
        when(step2.getEstimatedMinutes()).thenReturn(10);

        UserLearningProgress progress1 = mock(UserLearningProgress.class);
        when(progress1.getLearningStep()).thenReturn(step1);
        when(progress1.getScore()).thenReturn(93);
        when(progress1.getLearningStatus()).thenReturn("COMPLETED");

        when(learningRepository.findByIdAndIsActiveTrue(learningId)).thenReturn(Optional.of(learning));
        when(learningStepRepository.countByLearningId(learningId)).thenReturn(2L);
        when(userLearningProgressRepository.countCompletedStepsByUserIdAndLearningId(userId, learningId)).thenReturn(1L);
        when(learningStepRepository.findByLearning_IdOrderByStepNoAsc(learningId)).thenReturn(List.of(step1, step2));
        when(userLearningProgressRepository.findByUser_UserIdAndLearning_Id(userId, learningId)).thenReturn(List.of(progress1));

        LearningCurriculumResponseDTO.CurriculumResultDTO result = learningService.getCurriculum(userId, learningId);

        assertThat(result.title()).isEqualTo("Tension Notes");
        assertThat(result.progress().completedStepCount()).isEqualTo(1);
        assertThat(result.progress().totalStepCount()).isEqualTo(2);
        assertThat(result.progress().progressRate()).isEqualTo(50);
        assertThat(result.steps()).hasSize(2);
        assertThat(result.steps().get(0).status()).isEqualTo("COMPLETED");
        assertThat(result.steps().get(0).score()).isEqualTo(93);
        assertThat(result.steps().get(1).status()).isEqualTo("NOT_STARTED");
        assertThat(result.steps().get(1).score()).isNull();
    }

    @Test
    void 커리큘럼_조회_실패_학습_없음() {
        when(learningRepository.findByIdAndIsActiveTrue(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> learningService.getCurriculum(1L, 999L))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode())
                        .isEqualTo(LearningErrorStatus.LEARNING_NOT_FOUND));
    }
}
