package com.mr.domain.learning.service;

import com.mr.domain.learning.dto.req.LearningResultSaveRequestDTO;
import com.mr.domain.learning.dto.res.LearningAccompanimentListResponseDTO;
import com.mr.domain.learning.dto.res.LearningCurriculumResponseDTO;
import com.mr.domain.learning.dto.res.LearningHomeResponseDTO;
import com.mr.domain.learning.dto.res.LearningPracticeDataResponseDTO;
import com.mr.domain.learning.dto.res.LearningProgressResponseDTO;
import com.mr.domain.learning.dto.res.LearningResultResponseDTO;
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
import com.mr.domain.user.entity.User;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LearningService {

    private final UserLearningProgressRepository userLearningProgressRepository;
    private final LearningStepRepository learningStepRepository;
    private final LearningRepository learningRepository;
    private final PlayingExampleRepository playingExampleRepository;
    private final ChordExampleRepository chordExampleRepository;
    // 임시 작명
    private final UserRepository userRepository;

    // 학습 결과 저장
    @Transactional
    public LearningResultResponseDTO.SaveResultResultDTO saveResult(
            Long userId,
            Long learningId,
            LearningResultSaveRequestDTO.SaveResultDTO request
    ){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        Learning learning = learningRepository.findById(learningId)
                .orElseThrow(() -> new GeneralException(LearningErrorStatus.LEARNING_NOT_FOUND));

        LearningStep learningStep = learningStepRepository.findById(request.learningStepId())
                .orElseThrow(() -> new GeneralException(LearningErrorStatus.LEARNING_STEP_NOT_FOUND));

        learningStep.validateBelongsTo(learning);

        UserLearningProgress progress = userLearningProgressRepository
                .findByUser_UserIdAndLearningStep_Id(userId, request.learningStepId())
                .map(p -> {
                    p.updateProgress(request.score(), LocalDateTime.now());
                    return p;
                })
                .orElseGet(() -> {
                    UserLearningProgress newProgress = UserLearningProgress.create(user, learning, learningStep);
                    newProgress.updateProgress(request.score(), LocalDateTime.now());
                    return userLearningProgressRepository.save(newProgress);
                });
        return LearningResultResponseDTO.SaveResultResultDTO.from(progress);
    }

    // 학습 진행률 조회 로직
    public LearningProgressResponseDTO.ProgressResultDTO getLearningProgress(
            Long userId,
            Long learningId
    ) {
        // 학습 존재 여부 확인
        if (!learningRepository.existsById(learningId)) {
            throw new GeneralException(LearningErrorStatus.LEARNING_NOT_FOUND);
        }
        // 전체 학습 단계 수 조회
        long totalStepCount = learningStepRepository.countByLearningId(learningId);

        // 완료한 학습 단계 수 조회
        long completedStepCount = userLearningProgressRepository.countCompletedStepsByUserIdAndLearningId(userId, learningId);

        // 진행률 계산 (0으로 나누기 예외 방지)
        int progressRate = totalStepCount == 0 ? 0 : (int) Math.round((double) completedStepCount / totalStepCount * 100);

        return LearningProgressResponseDTO.ProgressResultDTO.of(learningId, progressRate);
    }

    // 학습 단계별 연습 실행 정보 조회
    public LearningPracticeDataResponseDTO.PracticeDataResultDTO getPracticeData(
            Long learningId,
            Long learningStepId
    ) {
        Learning learning = getActiveLearningOrThrow(learningId);
        LearningStep learningStep = getLearningStepOrThrow(learning, learningStepId);

        PlayingExample playingExample = playingExampleRepository.findByLearningStep_Id(learningStep.getId())
                .orElseThrow(() -> new GeneralException(LearningErrorStatus.PLAYING_EXAMPLE_NOT_FOUND));

        return LearningPracticeDataResponseDTO.PracticeDataResultDTO.from(playingExample);
    }

    // 학습 주제(THEORY) 전체보기
    public LearningTheoryListResponseDTO.TheoryListResultDTO getTheoryList(Long userId, String difficulty) {
        LearningDifficulty parsedDifficulty = parseDifficulty(difficulty);
        ensureUserExists(userId);

        List<Learning> learnings = learningRepository
                .findByCategoryAndDifficultyAndIsActiveTrueOrderByTitleAsc(LearningCategory.THEORY, parsedDifficulty);

        return LearningTheoryListResponseDTO.TheoryListResultDTO.from(learnings);
    }

    // 학습 커리큘럼 조회
    public LearningCurriculumResponseDTO.CurriculumResultDTO getCurriculum(Long userId, Long learningId) {
        ensureUserExists(userId);

        Learning learning = getActiveLearningOrThrow(learningId);

        long totalStepCount = learningStepRepository.countByLearningId(learningId);
        long completedStepCount = userLearningProgressRepository.countCompletedStepsByUserIdAndLearningId(userId, learningId);

        List<LearningStep> steps = learningStepRepository.findByLearning_IdOrderByStepNoAsc(learningId);
        Map<Long, UserLearningProgress> progressByStepId = userLearningProgressRepository
                .findByUser_UserIdAndLearning_Id(userId, learningId).stream()
                .collect(Collectors.toMap(p -> p.getLearningStep().getId(), Function.identity()));

        List<LearningCurriculumResponseDTO.StepItem> stepItems = steps.stream()
                .map(step -> toStepItem(step, progressByStepId.get(step.getId())))
                .toList();

        return LearningCurriculumResponseDTO.CurriculumResultDTO.of(
                learning,
                LearningCurriculumResponseDTO.ProgressInfo.of(completedStepCount, totalStepCount),
                stepItems
        );
    }

    private LearningCurriculumResponseDTO.StepItem toStepItem(LearningStep step, UserLearningProgress progress) {
        String status = progress != null ? progress.getLearningStatus() : "NOT_STARTED";
        Integer score = progress != null ? progress.getScore() : null;
        return LearningCurriculumResponseDTO.StepItem.of(step, status, score);
    }

    // 학습 단계별 조회
    public LearningStepDetailResponseDTO.StepDetailResultDTO getStepDetail(Long learningId, Long learningStepId) {
        Learning learning = getActiveLearningOrThrow(learningId);
        LearningStep learningStep = getLearningStepOrThrow(learning, learningStepId);

        PlayingExample playingExample = playingExampleRepository.findByLearningStep_Id(learningStepId).orElse(null);
        List<ChordExample> chordExamples = chordExampleRepository.findByLearningStep_Id(learningStepId);

        return LearningStepDetailResponseDTO.StepDetailResultDTO.of(learning, learningStep, playingExample, chordExamples);
    }

    // 실전 반주법 패키지(ACCOMPANIMENT) 전체보기
    public LearningAccompanimentListResponseDTO.AccompanimentListResultDTO getAccompanimentList(Long userId) {
        ensureUserExists(userId);

        List<Learning> learnings = learningRepository
                .findByCategoryAndIsActiveTrueOrderByTitleAsc(LearningCategory.ACCOMPANIMENT);

        List<LearningAccompanimentListResponseDTO.AccompanimentItem> items = toAccompanimentItems(userId, learnings);

        return LearningAccompanimentListResponseDTO.AccompanimentListResultDTO.of(items);
    }

    // 학습 홈 조회
    public LearningHomeResponseDTO.HomeResultDTO getHome(Long userId) {
        ensureUserExists(userId);

        LearningHomeResponseDTO.CurrentLearning currentLearning = buildCurrentLearning(userId);

        List<LearningHomeResponseDTO.TheoryPackageItem> theoryPackages = Stream.of(
                        LearningDifficulty.BEGINNER, LearningDifficulty.INTERMEDIATE, LearningDifficulty.ADVANCED)
                .flatMap(difficulty -> learningRepository
                        .findFirstByCategoryAndDifficultyAndIsActiveTrueOrderByTitleAsc(LearningCategory.THEORY, difficulty)
                        .stream())
                .map(LearningHomeResponseDTO.TheoryPackageItem::from)
                .toList();

        List<Learning> accompanimentLearnings = learningRepository
                .findTop3ByCategoryAndIsActiveTrueOrderByTitleAsc(LearningCategory.ACCOMPANIMENT);
        List<LearningAccompanimentListResponseDTO.AccompanimentItem> accompanimentPackages =
                toAccompanimentItems(userId, accompanimentLearnings);

        return LearningHomeResponseDTO.HomeResultDTO.of(currentLearning, theoryPackages, accompanimentPackages);
    }

    private LearningHomeResponseDTO.CurrentLearning buildCurrentLearning(Long userId) {
        return userLearningProgressRepository.findFirstByUser_UserIdAndLearning_IsActiveTrueOrderByLastStudiedAtDescIdDesc(userId)
                .map(latest -> {
                    Learning learning = latest.getLearning();
                    long totalStepCount = learningStepRepository.countByLearningId(learning.getId());
                    long completedStepCount = userLearningProgressRepository
                            .countCompletedStepsByUserIdAndLearningId(userId, learning.getId());
                    int progressRate = resolveProgressRate(completedStepCount, totalStepCount);

                    // 진행률 0%/100%면 홈 화면에 카드 자체를 숨김
                    if (progressRate == 0 || progressRate == 100) {
                        return null;
                    }

                    Long nextStepId = resolveNextStepId(userId, learning.getId(), latest.getLearningStep(), latest.getScore());

                    return LearningHomeResponseDTO.CurrentLearning.of(
                            learning,
                            latest.getLearningStep().getTitle(),
                            progressRate,
                            nextStepId);
                })
                .orElse(null);
    }

    // [이어서 학습하기] 클릭 시 이동할 단계 계산
    private Long resolveNextStepId(Long userId, Long learningId, LearningStep lastStep, Integer lastScore) {
        if (isStepIncomplete(lastScore)) {
            return lastStep.getId();
        }

        List<LearningStep> steps = learningStepRepository.findByLearning_IdOrderByStepNoAsc(learningId);
        Map<Long, Integer> scoreByStepId = userLearningProgressRepository
                .findByUser_UserIdAndLearning_Id(userId, learningId).stream()
                .collect(Collectors.toMap(p -> p.getLearningStep().getId(), UserLearningProgress::getScore));

        return steps.stream()
                .filter(step -> step.getStepNo() > lastStep.getStepNo())
                .filter(step -> isStepIncomplete(scoreByStepId.get(step.getId())))
                .findFirst()
                .or(() -> steps.stream()
                        .filter(step -> isStepIncomplete(scoreByStepId.get(step.getId())))
                        .findFirst())
                .map(LearningStep::getId)
                .orElse(null);
    }

    private boolean isStepIncomplete(Integer score) {
        return score == null || score < 90;
    }

    private List<LearningAccompanimentListResponseDTO.AccompanimentItem> toAccompanimentItems(
            Long userId, List<Learning> learnings) {
        Map<Long, Integer> progressRateByLearningId = buildProgressRateMap(userId, learnings);

        return learnings.stream()
                .map(learning -> LearningAccompanimentListResponseDTO.AccompanimentItem.of(
                        learning, progressRateByLearningId.getOrDefault(learning.getId(), 0)))
                .toList();
    }

    // 여러 학습에 대한 패키지 단위 진행률(%)을 한 번에 계산 (N+1 방지, 배치 집계 쿼리 재사용)
    private Map<Long, Integer> buildProgressRateMap(Long userId, List<Learning> learnings) {
        if (learnings.isEmpty()) {
            return Map.of();
        }

        List<Long> learningIds = learnings.stream().map(Learning::getId).toList();

        Map<Long, Long> totalStepCountByLearningId = learningStepRepository.countByLearningIdIn(learningIds).stream()
                .collect(Collectors.toMap(
                        LearningStepRepository.LearningIdCount::getLearningId,
                        LearningStepRepository.LearningIdCount::getStepCount));

        Map<Long, Long> completedStepCountByLearningId = userLearningProgressRepository
                .countCompletedStepsByUserIdAndLearningIdIn(userId, learningIds).stream()
                .collect(Collectors.toMap(
                        UserLearningProgressRepository.CompletedStepCount::getLearningId,
                        UserLearningProgressRepository.CompletedStepCount::getCompletedStepCount));

        return learningIds.stream().collect(Collectors.toMap(
                id -> id,
                id -> resolveProgressRate(
                        completedStepCountByLearningId.getOrDefault(id, 0L),
                        totalStepCountByLearningId.getOrDefault(id, 0L))));
    }

    private int resolveProgressRate(long completedStepCount, long totalStepCount) {
        if (totalStepCount == 0) {
            return 0;
        }
        int progressRate = (int) Math.round((double) completedStepCount / totalStepCount * 100);
        return Math.min(100, Math.max(0, progressRate));
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new GeneralException(UserErrorStatus.USER_NOT_FOUND);
        }
    }

    private LearningDifficulty parseDifficulty(String difficulty) {
        if (difficulty == null) {
            throw new GeneralException(LearningErrorStatus.INVALID_DIFFICULTY);
        }
        try {
            return LearningDifficulty.valueOf(difficulty.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new GeneralException(LearningErrorStatus.INVALID_DIFFICULTY);
        }
    }

    private Learning getActiveLearningOrThrow(Long learningId) {
        return learningRepository.findByIdAndIsActiveTrue(learningId)
                .orElseThrow(() -> new GeneralException(LearningErrorStatus.LEARNING_NOT_FOUND));
    }

    private LearningStep getLearningStepOrThrow(Learning learning, Long learningStepId) {
        LearningStep learningStep = learningStepRepository.findById(learningStepId)
                .orElseThrow(() -> new GeneralException(LearningErrorStatus.LEARNING_STEP_NOT_FOUND));

        if (!learningStep.getLearning().getId().equals(learning.getId())) {
            throw new GeneralException(LearningErrorStatus.LEARNING_STEP_NOT_FOUND);
        }
        return learningStep;
    }
}
