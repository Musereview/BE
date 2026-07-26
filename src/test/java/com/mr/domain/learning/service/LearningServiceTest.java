package com.mr.domain.learning.service;

import com.mr.domain.learning.dto.res.LearningAccompanimentListResponseDTO;
import com.mr.domain.learning.dto.res.LearningCurriculumResponseDTO;
import com.mr.domain.learning.dto.res.LearningHomeResponseDTO;
import com.mr.domain.learning.dto.res.LearningStepDetailResponseDTO;
import com.mr.domain.learning.dto.res.LearningTheoryListResponseDTO;
import com.mr.domain.learning.entity.ChordExample;
import com.mr.domain.learning.entity.Learning;
import com.mr.domain.learning.entity.LearningStep;
import com.mr.domain.learning.entity.PlayingExample;
import com.mr.domain.learning.entity.UserLearningProgress;
import com.mr.domain.learning.entity.enums.LearningCategory;
import com.mr.domain.learning.entity.enums.LearningDifficulty;
import com.mr.domain.learning.exception.LearningErrorStatus;
import com.mr.domain.learning.repository.ChordExampleRepository;
import com.mr.domain.learning.repository.LearningRepository;
import com.mr.domain.learning.repository.LearningStepRepository;
import com.mr.domain.learning.repository.PlayingExampleRepository;
import com.mr.domain.learning.repository.UserLearningProgressRepository;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    private ChordExampleRepository chordExampleRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LearningService learningService;

    @Test
    @DisplayName("getTheoryList - 소문자와 공백이 포함된 difficulty도 정상 변환된다")
    void getTheoryList_lowercaseAndWhitespaceDifficulty_normalizedSuccessfully() {
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
    @DisplayName("getTheoryList - 존재하지 않는 difficulty 값이면 400")
    void getTheoryList_invalidDifficulty_throws400() {
        assertThatThrownBy(() -> learningService.getTheoryList(1L, "invalid"))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode())
                        .isEqualTo(LearningErrorStatus.INVALID_DIFFICULTY));
    }

    @Test
    @DisplayName("getCurriculum - 진행률과 단계별 상태를 계산해 반환한다")
    void getCurriculum_success_calculatesProgressAndStepStatus() {
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

        when(userRepository.existsById(userId)).thenReturn(true);
        when(learningRepository.findByIdAndIsActiveTrue(learningId)).thenReturn(Optional.of(learning));
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
    @DisplayName("getCurriculum - 완료 단계 수가 전체 단계 수보다 많은 데이터 이상이어도 progressRate는 100을 넘지 않는다")
    void getCurriculum_completedExceedsTotal_progressRateClampedTo100() {
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
        when(step1.getTitle()).thenReturn("스텝1");
        when(step1.getSummary()).thenReturn("설명1");
        when(step1.getEstimatedMinutes()).thenReturn(10);

        LearningStep step2 = mock(LearningStep.class);
        when(step2.getId()).thenReturn(12L);
        when(step2.getStepNo()).thenReturn(2);
        when(step2.getTitle()).thenReturn("스텝2");
        when(step2.getSummary()).thenReturn("설명2");
        when(step2.getEstimatedMinutes()).thenReturn(10);

        // 완료 기록(score>=90) 3건이 실제 단계 수(2개)보다 많은 데이터 이상 상황을 가정
        UserLearningProgress progress1 = mock(UserLearningProgress.class);
        when(progress1.getLearningStep()).thenReturn(step1);
        when(progress1.getScore()).thenReturn(95);
        when(progress1.getLearningStatus()).thenReturn("COMPLETED");

        UserLearningProgress progress2 = mock(UserLearningProgress.class);
        when(progress2.getLearningStep()).thenReturn(step2);
        when(progress2.getScore()).thenReturn(95);
        when(progress2.getLearningStatus()).thenReturn("COMPLETED");

        LearningStep orphanStep = mock(LearningStep.class);
        when(orphanStep.getId()).thenReturn(13L);
        UserLearningProgress progress3 = mock(UserLearningProgress.class);
        when(progress3.getLearningStep()).thenReturn(orphanStep);
        when(progress3.getScore()).thenReturn(95);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(learningRepository.findByIdAndIsActiveTrue(learningId)).thenReturn(Optional.of(learning));
        when(learningStepRepository.findByLearning_IdOrderByStepNoAsc(learningId)).thenReturn(List.of(step1, step2));
        when(userLearningProgressRepository.findByUser_UserIdAndLearning_Id(userId, learningId))
                .thenReturn(List.of(progress1, progress2, progress3));

        LearningCurriculumResponseDTO.CurriculumResultDTO result = learningService.getCurriculum(userId, learningId);

        assertThat(result.progress().totalStepCount()).isEqualTo(2);
        assertThat(result.progress().completedStepCount()).isEqualTo(3);
        assertThat(result.progress().progressRate()).isEqualTo(100);
    }

    @Test
    @DisplayName("getCurriculum - 학습이 없으면 404")
    void getCurriculum_learningNotFound_throws404() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(learningRepository.findByIdAndIsActiveTrue(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> learningService.getCurriculum(1L, 999L))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode())
                        .isEqualTo(LearningErrorStatus.LEARNING_NOT_FOUND));
    }

    @Test
    @DisplayName("getCurriculum - 유저가 없으면 404")
    void getCurriculum_userNotFound_throws404() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> learningService.getCurriculum(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode())
                        .isEqualTo(UserErrorStatus.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("getStepDetail - 모범연주와 코드예시를 포함해 반환한다")
    void getStepDetail_success_includesModelPerformanceAndChordExamples() {
        Long learningId = 1L;
        Long learningStepId = 12L;

        Learning learning = mock(Learning.class);
        when(learning.getId()).thenReturn(learningId);
        when(learning.getTitle()).thenReturn("Tension Notes");
        when(learning.getDifficulty()).thenReturn(LearningDifficulty.ADVANCED);

        LearningStep step = mock(LearningStep.class);
        when(step.getId()).thenReturn(learningStepId);
        when(step.getLearning()).thenReturn(learning);
        when(step.getStepNo()).thenReturn(2);
        when(step.getTitle()).thenReturn("11th 텐션 노트 활용하기");
        when(step.getContent()).thenReturn("이론 설명");
        when(step.getPracticeTip()).thenReturn("연습 팁");

        PlayingExample playingExample = mock(PlayingExample.class);
        when(playingExample.getTitle()).thenReturn("11th Tension Notes Practice");
        when(playingExample.getDescription()).thenReturn("프로 연주자의 응용 사례");
        when(playingExample.getAudioFileUrl()).thenReturn("https://cdn.example.com/audio/11th.mp3");
        when(playingExample.getPlayingSeconds()).thenReturn(154L);

        ChordExample chordExample = mock(ChordExample.class);
        when(chordExample.getChordName()).thenReturn("Cmaj7");
        when(chordExample.getDescription()).thenReturn("F(11th) 주의 - E(3rd)와 충돌");
        when(chordExample.getNoteNumbers()).thenReturn(List.of(60, 64, 67, 70, 77));

        when(learningRepository.findByIdAndIsActiveTrue(learningId)).thenReturn(Optional.of(learning));
        when(learningStepRepository.findById(learningStepId)).thenReturn(Optional.of(step));
        when(playingExampleRepository.findByLearningStep_Id(learningStepId)).thenReturn(Optional.of(playingExample));
        when(chordExampleRepository.findByLearningStep_Id(learningStepId)).thenReturn(List.of(chordExample));

        LearningStepDetailResponseDTO.StepDetailResultDTO result =
                learningService.getStepDetail(learningId, learningStepId);

        assertThat(result.stepTitle()).isEqualTo("11th 텐션 노트 활용하기");
        assertThat(result.modelPerformance()).isNotNull();
        assertThat(result.modelPerformance().durationSeconds()).isEqualTo(154);
        assertThat(result.chordExamples()).hasSize(1);
        assertThat(result.chordExamples().get(0).chordName()).isEqualTo("Cmaj7");
    }

    @Test
    @DisplayName("getStepDetail - 모범연주가 없으면 null을 반환한다")
    void getStepDetail_noModelPerformance_returnsNull() {
        Long learningId = 1L;
        Long learningStepId = 13L;

        Learning learning = mock(Learning.class);
        when(learning.getId()).thenReturn(learningId);
        when(learning.getDifficulty()).thenReturn(LearningDifficulty.ADVANCED);

        LearningStep step = mock(LearningStep.class);
        when(step.getId()).thenReturn(learningStepId);
        when(step.getLearning()).thenReturn(learning);

        when(learningRepository.findByIdAndIsActiveTrue(learningId)).thenReturn(Optional.of(learning));
        when(learningStepRepository.findById(learningStepId)).thenReturn(Optional.of(step));
        when(playingExampleRepository.findByLearningStep_Id(learningStepId)).thenReturn(Optional.empty());
        when(chordExampleRepository.findByLearningStep_Id(learningStepId)).thenReturn(Collections.emptyList());

        LearningStepDetailResponseDTO.StepDetailResultDTO result =
                learningService.getStepDetail(learningId, learningStepId);

        assertThat(result.modelPerformance()).isNull();
        assertThat(result.chordExamples()).isEmpty();
    }

    @Test
    @DisplayName("getStepDetail - step이 다른 learning 소속이면 404")
    void getStepDetail_stepBelongsToOtherLearning_throws404() {
        Long learningId = 1L;
        Long learningStepId = 99L;

        Learning learning = mock(Learning.class);
        when(learning.getId()).thenReturn(learningId);

        Learning otherLearning = mock(Learning.class);
        when(otherLearning.getId()).thenReturn(2L);

        LearningStep step = mock(LearningStep.class);
        when(step.getLearning()).thenReturn(otherLearning);

        when(learningRepository.findByIdAndIsActiveTrue(learningId)).thenReturn(Optional.of(learning));
        when(learningStepRepository.findById(learningStepId)).thenReturn(Optional.of(step));

        assertThatThrownBy(() -> learningService.getStepDetail(learningId, learningStepId))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode())
                        .isEqualTo(LearningErrorStatus.LEARNING_STEP_NOT_FOUND));
    }

    @Test
    @DisplayName("getAccompanimentList - 패키지별 진행률을 계산해 반환한다")
    void getAccompanimentList_success_calculatesProgressPerPackage() {
        Long userId = 1L;

        Learning chapter1 = mock(Learning.class);
        when(chapter1.getId()).thenReturn(5L);
        when(chapter1.getTitle()).thenReturn("Chapter 1");
        when(chapter1.getSummary()).thenReturn("설명1");
        when(chapter1.getEstimatedMinutes()).thenReturn(10);

        Learning chapter2 = mock(Learning.class);
        when(chapter2.getId()).thenReturn(6L);
        when(chapter2.getTitle()).thenReturn("Chapter 2");
        when(chapter2.getSummary()).thenReturn("설명2");
        when(chapter2.getEstimatedMinutes()).thenReturn(10);

        LearningStepRepository.LearningIdCount totalCh1 = mock(LearningStepRepository.LearningIdCount.class);
        when(totalCh1.getLearningId()).thenReturn(5L);
        when(totalCh1.getStepCount()).thenReturn(4L);
        LearningStepRepository.LearningIdCount totalCh2 = mock(LearningStepRepository.LearningIdCount.class);
        when(totalCh2.getLearningId()).thenReturn(6L);
        when(totalCh2.getStepCount()).thenReturn(5L);

        UserLearningProgressRepository.CompletedStepCount completedCh1 =
                mock(UserLearningProgressRepository.CompletedStepCount.class);
        when(completedCh1.getLearningId()).thenReturn(5L);
        when(completedCh1.getCompletedStepCount()).thenReturn(4L);
        // chapter2는 진행 기록 없음 → completed map에 아예 없음(0으로 처리돼야 함)

        when(userRepository.existsById(userId)).thenReturn(true);
        when(learningRepository.findByCategoryAndIsActiveTrueOrderByTitleAsc(LearningCategory.ACCOMPANIMENT))
                .thenReturn(List.of(chapter1, chapter2));
        when(learningStepRepository.countByLearningIdIn(List.of(5L, 6L)))
                .thenReturn(List.of(totalCh1, totalCh2));
        when(userLearningProgressRepository.countCompletedStepsByUserIdAndLearningIdIn(userId, List.of(5L, 6L)))
                .thenReturn(List.of(completedCh1));

        LearningAccompanimentListResponseDTO.AccompanimentListResultDTO result =
                learningService.getAccompanimentList(userId);

        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.items().get(0).progressRate()).isEqualTo(100);
        assertThat(result.items().get(1).progressRate()).isEqualTo(0);
    }

    @Test
    @DisplayName("getAccompanimentList - 완료 단계 수가 전체 단계 수보다 많은 데이터 이상이어도 progressRate는 100을 넘지 않는다")
    void getAccompanimentList_completedExceedsTotal_progressRateClampedTo100() {
        Long userId = 1L;

        Learning chapter1 = mock(Learning.class);
        when(chapter1.getId()).thenReturn(5L);
        when(chapter1.getTitle()).thenReturn("Chapter 1");
        when(chapter1.getSummary()).thenReturn("설명1");
        when(chapter1.getEstimatedMinutes()).thenReturn(10);

        LearningStepRepository.LearningIdCount totalCh1 = mock(LearningStepRepository.LearningIdCount.class);
        when(totalCh1.getLearningId()).thenReturn(5L);
        when(totalCh1.getStepCount()).thenReturn(4L);

        UserLearningProgressRepository.CompletedStepCount completedCh1 =
                mock(UserLearningProgressRepository.CompletedStepCount.class);
        when(completedCh1.getLearningId()).thenReturn(5L);
        when(completedCh1.getCompletedStepCount()).thenReturn(6L);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(learningRepository.findByCategoryAndIsActiveTrueOrderByTitleAsc(LearningCategory.ACCOMPANIMENT))
                .thenReturn(List.of(chapter1));
        when(learningStepRepository.countByLearningIdIn(List.of(5L)))
                .thenReturn(List.of(totalCh1));
        when(userLearningProgressRepository.countCompletedStepsByUserIdAndLearningIdIn(userId, List.of(5L)))
                .thenReturn(List.of(completedCh1));

        LearningAccompanimentListResponseDTO.AccompanimentListResultDTO result =
                learningService.getAccompanimentList(userId);

        assertThat(result.items().get(0).progressRate()).isEqualTo(100);
    }

    @Test
    @DisplayName("getAccompanimentList - 결과가 없으면 집계 쿼리를 하지 않는다")
    void getAccompanimentList_empty_doesNotQueryAggregation() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(learningRepository.findByCategoryAndIsActiveTrueOrderByTitleAsc(LearningCategory.ACCOMPANIMENT))
                .thenReturn(Collections.emptyList());

        LearningAccompanimentListResponseDTO.AccompanimentListResultDTO result =
                learningService.getAccompanimentList(1L);

        assertThat(result.totalCount()).isZero();
        assertThat(result.items()).isEmpty();
        verify(learningStepRepository, never()).countByLearningIdIn(anyList());
        verify(userLearningProgressRepository, never()).countCompletedStepsByUserIdAndLearningIdIn(anyLong(), anyList());
    }

    @Test
    @DisplayName("getAccompanimentList - 유저가 없으면 404")
    void getAccompanimentList_userNotFound_throws404() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> learningService.getAccompanimentList(1L))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode())
                        .isEqualTo(UserErrorStatus.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("getHome - 최근 학습을 포함해 반환한다")
    void getHome_success_includesRecentLearning() {
        Long userId = 1L;

        Learning currentPackage = mock(Learning.class);
        when(currentPackage.getId()).thenReturn(1L);
        when(currentPackage.getTitle()).thenReturn("Tension Notes");
        when(currentPackage.getDifficulty()).thenReturn(LearningDifficulty.ADVANCED);

        LearningStep lastStep = mock(LearningStep.class);
        when(lastStep.getId()).thenReturn(12L);
        when(lastStep.getStepNo()).thenReturn(2);
        when(lastStep.getTitle()).thenReturn("11th 텐션 노트 활용하기");

        UserLearningProgress latest = mock(UserLearningProgress.class);
        when(latest.getLearning()).thenReturn(currentPackage);
        when(latest.getLearningStep()).thenReturn(lastStep);
        when(latest.getScore()).thenReturn(93);

        LearningStep step1 = mock(LearningStep.class);
        when(step1.getStepNo()).thenReturn(1);
        LearningStep step3 = mock(LearningStep.class);
        when(step3.getId()).thenReturn(13L);
        when(step3.getStepNo()).thenReturn(3);
        LearningStep step4 = mock(LearningStep.class);

        UserLearningProgress step2Progress = mock(UserLearningProgress.class);
        when(step2Progress.getLearningStep()).thenReturn(lastStep);
        when(step2Progress.getScore()).thenReturn(93);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(userLearningProgressRepository.findFirstByUser_UserIdAndLearning_IsActiveTrueOrderByLastStudiedAtDescIdDesc(userId))
                .thenReturn(Optional.of(latest));
        when(learningStepRepository.countByLearningId(1L)).thenReturn(4L);
        when(userLearningProgressRepository.countCompletedStepsByUserIdAndLearningId(userId, 1L)).thenReturn(1L);
        when(learningStepRepository.findByLearning_IdOrderByStepNoAsc(1L))
                .thenReturn(List.of(step1, lastStep, step3, step4));
        when(userLearningProgressRepository.findByUser_UserIdAndLearning_Id(userId, 1L))
                .thenReturn(List.of(step2Progress));

        Learning beginnerTheory = mock(Learning.class);
        when(beginnerTheory.getId()).thenReturn(2L);
        when(beginnerTheory.getDifficulty()).thenReturn(LearningDifficulty.BEGINNER);
        when(learningRepository.findFirstByCategoryAndDifficultyAndIsActiveTrueOrderByTitleAsc(
                LearningCategory.THEORY, LearningDifficulty.BEGINNER)).thenReturn(Optional.of(beginnerTheory));
        when(learningRepository.findFirstByCategoryAndDifficultyAndIsActiveTrueOrderByTitleAsc(
                LearningCategory.THEORY, LearningDifficulty.INTERMEDIATE)).thenReturn(Optional.empty());
        when(learningRepository.findFirstByCategoryAndDifficultyAndIsActiveTrueOrderByTitleAsc(
                LearningCategory.THEORY, LearningDifficulty.ADVANCED)).thenReturn(Optional.empty());

        when(learningRepository.findTop3ByCategoryAndIsActiveTrueOrderByTitleAsc(LearningCategory.ACCOMPANIMENT))
                .thenReturn(Collections.emptyList());

        LearningHomeResponseDTO.HomeResultDTO result = learningService.getHome(userId);

        assertThat(result.currentLearning()).isNotNull();
        assertThat(result.currentLearning().stepTitle()).isEqualTo("11th 텐션 노트 활용하기");
        assertThat(result.currentLearning().progressRate()).isEqualTo(25);
        assertThat(result.currentLearning().nextStepId()).isEqualTo(13L);
        assertThat(result.theoryPackages()).hasSize(1);
        assertThat(result.accompanimentPackages()).isEmpty();
    }

    @Test
    @DisplayName("getHome - 최근 학습 기록이 없으면 currentLearning은 null이다")
    void getHome_noRecentLearning_currentLearningIsNull() {
        Long userId = 1L;

        when(userRepository.existsById(userId)).thenReturn(true);
        when(userLearningProgressRepository.findFirstByUser_UserIdAndLearning_IsActiveTrueOrderByLastStudiedAtDescIdDesc(userId))
                .thenReturn(Optional.empty());
        when(learningRepository.findFirstByCategoryAndDifficultyAndIsActiveTrueOrderByTitleAsc(
                eq(LearningCategory.THEORY), any()))
                .thenReturn(Optional.empty());
        when(learningRepository.findTop3ByCategoryAndIsActiveTrueOrderByTitleAsc(LearningCategory.ACCOMPANIMENT))
                .thenReturn(Collections.emptyList());

        LearningHomeResponseDTO.HomeResultDTO result = learningService.getHome(userId);

        assertThat(result.currentLearning()).isNull();
    }

    @Test
    @DisplayName("getHome - 최근 학습 패키지 진행률이 100%면 currentLearning은 null이다")
    void getHome_progressIs100Percent_currentLearningIsNull() {
        Long userId = 1L;

        Learning currentPackage = mock(Learning.class);
        when(currentPackage.getId()).thenReturn(1L);

        UserLearningProgress latest = mock(UserLearningProgress.class);
        when(latest.getLearning()).thenReturn(currentPackage);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(userLearningProgressRepository.findFirstByUser_UserIdAndLearning_IsActiveTrueOrderByLastStudiedAtDescIdDesc(userId))
                .thenReturn(Optional.of(latest));
        when(learningStepRepository.countByLearningId(1L)).thenReturn(2L);
        when(userLearningProgressRepository.countCompletedStepsByUserIdAndLearningId(userId, 1L)).thenReturn(2L);
        when(learningRepository.findFirstByCategoryAndDifficultyAndIsActiveTrueOrderByTitleAsc(
                eq(LearningCategory.THEORY), any())).thenReturn(Optional.empty());
        when(learningRepository.findTop3ByCategoryAndIsActiveTrueOrderByTitleAsc(LearningCategory.ACCOMPANIMENT))
                .thenReturn(Collections.emptyList());

        LearningHomeResponseDTO.HomeResultDTO result = learningService.getHome(userId);

        assertThat(result.currentLearning()).isNull();
    }

    @Test
    @DisplayName("getHome - 최근 학습 패키지 진행률이 0%면 currentLearning은 null이다")
    void getHome_progressIs0Percent_currentLearningIsNull() {
        Long userId = 1L;

        Learning currentPackage = mock(Learning.class);
        when(currentPackage.getId()).thenReturn(1L);

        UserLearningProgress latest = mock(UserLearningProgress.class);
        when(latest.getLearning()).thenReturn(currentPackage);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(userLearningProgressRepository.findFirstByUser_UserIdAndLearning_IsActiveTrueOrderByLastStudiedAtDescIdDesc(userId))
                .thenReturn(Optional.of(latest));
        when(learningStepRepository.countByLearningId(1L)).thenReturn(4L);
        when(userLearningProgressRepository.countCompletedStepsByUserIdAndLearningId(userId, 1L)).thenReturn(0L);
        when(learningRepository.findFirstByCategoryAndDifficultyAndIsActiveTrueOrderByTitleAsc(
                eq(LearningCategory.THEORY), any())).thenReturn(Optional.empty());
        when(learningRepository.findTop3ByCategoryAndIsActiveTrueOrderByTitleAsc(LearningCategory.ACCOMPANIMENT))
                .thenReturn(Collections.emptyList());

        LearningHomeResponseDTO.HomeResultDTO result = learningService.getHome(userId);

        assertThat(result.currentLearning()).isNull();
    }

    @Test
    @DisplayName("getHome - 마지막 학습 단계 점수가 90점 미만이면 nextStepId는 그 단계 자신이다")
    void getHome_nextStepId_returnsSameStep_whenScoreBelow90() {
        Long userId = 1L;

        Learning currentPackage = mock(Learning.class);
        when(currentPackage.getId()).thenReturn(1L);
        when(currentPackage.getTitle()).thenReturn("Tension Notes");
        when(currentPackage.getDifficulty()).thenReturn(LearningDifficulty.ADVANCED);

        LearningStep lastStep = mock(LearningStep.class);
        when(lastStep.getId()).thenReturn(12L);
        when(lastStep.getTitle()).thenReturn("11th 텐션 노트 활용하기");

        UserLearningProgress latest = mock(UserLearningProgress.class);
        when(latest.getLearning()).thenReturn(currentPackage);
        when(latest.getLearningStep()).thenReturn(lastStep);
        when(latest.getScore()).thenReturn(50);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(userLearningProgressRepository.findFirstByUser_UserIdAndLearning_IsActiveTrueOrderByLastStudiedAtDescIdDesc(userId))
                .thenReturn(Optional.of(latest));
        when(learningStepRepository.countByLearningId(1L)).thenReturn(4L);
        when(userLearningProgressRepository.countCompletedStepsByUserIdAndLearningId(userId, 1L)).thenReturn(1L);
        when(learningRepository.findFirstByCategoryAndDifficultyAndIsActiveTrueOrderByTitleAsc(
                eq(LearningCategory.THEORY), any())).thenReturn(Optional.empty());
        when(learningRepository.findTop3ByCategoryAndIsActiveTrueOrderByTitleAsc(LearningCategory.ACCOMPANIMENT))
                .thenReturn(Collections.emptyList());

        LearningHomeResponseDTO.HomeResultDTO result = learningService.getHome(userId);

        assertThat(result.currentLearning().nextStepId()).isEqualTo(12L);
    }

    @Test
    @DisplayName("getHome - 뒤쪽에 미완료 단계가 없으면 앞쪽 미완료 단계로 폴백한다")
    void getHome_nextStepId_fallsBackToEarlierIncompleteStep_whenNoIncompleteStepAfter() {
        Long userId = 1L;

        Learning currentPackage = mock(Learning.class);
        when(currentPackage.getId()).thenReturn(1L);
        when(currentPackage.getTitle()).thenReturn("Tension Notes");
        when(currentPackage.getDifficulty()).thenReturn(LearningDifficulty.ADVANCED);

        LearningStep step1 = mock(LearningStep.class); // 미완료(NOT_STARTED)로 남아있는 앞쪽 단계
        when(step1.getId()).thenReturn(11L);
        when(step1.getStepNo()).thenReturn(1);

        LearningStep lastStep = mock(LearningStep.class); // 마지막 학습 = 패키지의 최종 단계(4번)
        when(lastStep.getId()).thenReturn(14L);
        when(lastStep.getStepNo()).thenReturn(4);
        when(lastStep.getTitle()).thenReturn("13th 텐션 노트 활용하기");

        UserLearningProgress latest = mock(UserLearningProgress.class);
        when(latest.getLearning()).thenReturn(currentPackage);
        when(latest.getLearningStep()).thenReturn(lastStep);
        when(latest.getScore()).thenReturn(93);

        UserLearningProgress step4Progress = mock(UserLearningProgress.class);
        when(step4Progress.getLearningStep()).thenReturn(lastStep);
        when(step4Progress.getScore()).thenReturn(93);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(userLearningProgressRepository.findFirstByUser_UserIdAndLearning_IsActiveTrueOrderByLastStudiedAtDescIdDesc(userId))
                .thenReturn(Optional.of(latest));
        when(learningStepRepository.countByLearningId(1L)).thenReturn(4L);
        when(userLearningProgressRepository.countCompletedStepsByUserIdAndLearningId(userId, 1L)).thenReturn(3L);
        when(learningStepRepository.findByLearning_IdOrderByStepNoAsc(1L))
                .thenReturn(List.of(step1, lastStep));
        when(userLearningProgressRepository.findByUser_UserIdAndLearning_Id(userId, 1L))
                .thenReturn(List.of(step4Progress));
        when(learningRepository.findFirstByCategoryAndDifficultyAndIsActiveTrueOrderByTitleAsc(
                eq(LearningCategory.THEORY), any())).thenReturn(Optional.empty());
        when(learningRepository.findTop3ByCategoryAndIsActiveTrueOrderByTitleAsc(LearningCategory.ACCOMPANIMENT))
                .thenReturn(Collections.emptyList());

        LearningHomeResponseDTO.HomeResultDTO result = learningService.getHome(userId);

        assertThat(result.currentLearning().nextStepId()).isEqualTo(11L);
    }

    @Test
    @DisplayName("getHome - 유저가 없으면 404")
    void getHome_userNotFound_throws404() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> learningService.getHome(1L))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode())
                        .isEqualTo(UserErrorStatus.USER_NOT_FOUND));
    }
}
