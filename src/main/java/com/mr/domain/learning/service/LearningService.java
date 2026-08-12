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
import com.mr.global.event.NotificationEvent;
import com.mr.global.file.s3.enums.S3FileType;
import com.mr.global.file.s3.service.S3FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LearningService {

    private static final int RECOMMENDED_LEARNING_LIMIT = 2;
    private static final List<LearningDifficulty> RECOMMENDATION_DIFFICULTY_ORDER =
            List.of(LearningDifficulty.BEGINNER, LearningDifficulty.INTERMEDIATE, LearningDifficulty.ADVANCED);

    private final UserLearningProgressRepository userLearningProgressRepository;
    private final LearningStepRepository learningStepRepository;
    private final LearningRepository learningRepository;
    private final PlayingExampleRepository playingExampleRepository;
    private final ChordExampleRepository chordExampleRepository;
    private final S3FileService s3FileService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LearningResultResponseDTO.SaveResultResultDTO saveResult(
            Long userId,
            Long learningId,
            LearningResultSaveRequestDTO.SaveResultDTO request
    ){
        // 같은 유저의 동시 저장 요청을 직렬화하기 위해 유저 행에 비관적 락을 걸고 조회
        // (completedStepCountBefore/After 판정 구간 전체가 이 락 보유 중에 실행되어야 TOCTOU가 안 생김.
        //  패키지가 아니라 유저 단위로 잠가서, 같은 패키지를 학습하는 다른 유저끼리는 잠기지 않는다)
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(()-> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        Learning learning = learningRepository.findByIdAndIsActiveTrue(learningId)
                .orElseThrow(() -> new GeneralException(LearningErrorStatus.LEARNING_NOT_FOUND));

        LearningStep learningStep = learningStepRepository.findById(request.learningStepId())
                .orElseThrow(() -> new GeneralException(LearningErrorStatus.LEARNING_STEP_NOT_FOUND));

        learningStep.validateBelongsTo(learning);

        // 이번 저장으로 패키지가 막 100% 완료되는지 판정하기 위해, 갱신 전 완료 개수를 먼저 스냅샷
        long totalStepCount = learningStepRepository.countByLearningId(learningId);
        long completedStepCountBefore = userLearningProgressRepository
                .countCompletedStepsByUserIdAndLearningId(userId, learningId);

        UserLearningProgress progress = userLearningProgressRepository
                .findByUser_UserIdAndLearningStep_Id(userId, request.learningStepId())
                .map(p -> {
                    p.updateProgress(request.score(), Instant.now());
                    return p;
                })
                .orElseGet(() -> {
                    UserLearningProgress newProgress = UserLearningProgress.create(user, learning, learningStep);
                    newProgress.updateProgress(request.score(), Instant.now());
                    return userLearningProgressRepository.save(newProgress);
                });

        notifyIfPackageJustCompleted(userId, learningId, learning, totalStepCount, completedStepCountBefore);

        return LearningResultResponseDTO.SaveResultResultDTO.from(progress);
    }

    // 이번 저장으로 패키지가 미완료 → 완료(100%)로 막 전환된 경우에만 완료 알림 발행.
    // 완료 여부는 저장 전/후 스냅샷을 산술로 조합하지 않고, 저장 이후 시점의 완료 개수를 다시 조회해 판정한다
    // (다른 단계가 동시에 저장되는 경우까지 고려해 실제 커밋된 값 기준으로 확인)
    private void notifyIfPackageJustCompleted(
            Long userId, Long learningId, Learning learning, long totalStepCount, long completedStepCountBefore
    ) {
        if (totalStepCount == 0 || completedStepCountBefore == totalStepCount) {
            return;
        }

        long completedStepCountAfter = userLearningProgressRepository
                .countCompletedStepsByUserIdAndLearningId(userId, learningId);

        if (completedStepCountAfter == totalStepCount) {
            String topicName = learning.getTitle() + " (" + learning.getDifficulty().getLabel() + ")";
            eventPublisher.publishEvent(NotificationEvent.forLearning(userId, topicName));
        }
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

    public LearningTheoryListResponseDTO.TheoryListResultDTO getTheoryList(Long userId, String difficulty) {
        LearningDifficulty parsedDifficulty = parseDifficulty(difficulty);
        ensureUserExists(userId);

        List<Learning> learnings = learningRepository
                .findByCategoryAndDifficultyAndIsActiveTrueOrderByTitleAsc(LearningCategory.THEORY, parsedDifficulty);

        return LearningTheoryListResponseDTO.TheoryListResultDTO.from(learnings);
    }

    public LearningCurriculumResponseDTO.CurriculumResultDTO getCurriculum(Long userId, Long learningId) {
        ensureUserExists(userId);

        Learning learning = getActiveLearningOrThrow(learningId);

        List<LearningStep> steps = learningStepRepository.findByLearning_IdOrderByStepNoAsc(learningId);
        List<UserLearningProgress> progressList = userLearningProgressRepository
                .findByUser_UserIdAndLearning_Id(userId, learningId);
        Map<Long, UserLearningProgress> progressByStepId = progressList.stream()
                .collect(Collectors.toMap(p -> p.getLearningStep().getId(), Function.identity()));

        long totalStepCount = steps.size();
        long completedStepCount = progressList.stream()
                .filter(p -> p.getScore() != null && p.getScore() >= 90)
                .count();

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

    public LearningStepDetailResponseDTO.StepDetailResultDTO getStepDetail(Long learningId, Long learningStepId) {
        Learning learning = getActiveLearningOrThrow(learningId);
        LearningStep learningStep = getLearningStepOrThrow(learning, learningStepId);

        PlayingExample playingExample = playingExampleRepository.findByLearningStep_Id(learningStepId).orElse(null);
        List<ChordExample> chordExamples = chordExampleRepository.findByLearningStep_Id(learningStepId);

        String audioUrl = playingExample == null
                ? null
                : s3FileService.createPresignedDownload(
                        S3FileType.PLAYING_EXAMPLE,
                        playingExample.getAudioObjectKey()
                );

        return LearningStepDetailResponseDTO.StepDetailResultDTO.of(
                learning,
                learningStep,
                playingExample,
                audioUrl,
                chordExamples
        );
    }

    public LearningAccompanimentListResponseDTO.AccompanimentListResultDTO getAccompanimentList(Long userId) {
        ensureUserExists(userId);

        List<Learning> learnings = learningRepository
                .findByCategoryAndIsActiveTrueOrderByTitleAsc(LearningCategory.ACCOMPANIMENT);

        List<LearningAccompanimentListResponseDTO.AccompanimentItem> items = toAccompanimentItems(userId, learnings);

        return LearningAccompanimentListResponseDTO.AccompanimentListResultDTO.of(items);
    }

    public LearningHomeResponseDTO.HomeResultDTO getHome(Long userId) {
        ensureUserExists(userId);

        LearningHomeResponseDTO.CurrentLearning currentLearning = getCurrentLearning(userId);

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

    // 최근 학습 이어서 하기. 재도전만 있어서 진행률 0%인 경우도 포함해서 보여준다(PM 확인 2026-08-10).
    // 가장 최근 패키지가 100% 완료(=resolveNextStepId가 null, 이어갈 단계 없음)면 건너뛰고, 최근 20건 중
    // 그 직전에 만졌던 미완료 패키지를 찾아 대신 보여준다 — 안 그러면 방금 다 끝낸 패키지 하나 때문에
    // 그 직전의 진짜 재도전 기록까지 통째로 숨어버림(PR #193 은우님 리뷰로 발견)
    public LearningHomeResponseDTO.CurrentLearning getCurrentLearning(Long userId) {
        List<UserLearningProgress> recentProgress = findRecentActiveProgress(userId);
        Set<Long> checkedLearningIds = new HashSet<>();

        for (UserLearningProgress progress : recentProgress) {
            Long learningId = progress.getLearning().getId();
            if (!checkedLearningIds.add(learningId)) {
                continue;
            }

            Long nextStepId = resolveNextStepId(userId, learningId, progress.getLearningStep(), progress.getScore());
            if (nextStepId == null) {
                continue;
            }

            long totalStepCount = learningStepRepository.countByLearningId(learningId);
            long completedStepCount = userLearningProgressRepository
                    .countCompletedStepsByUserIdAndLearningId(userId, learningId);
            int progressRate = resolveProgressRate(completedStepCount, totalStepCount);

            return LearningHomeResponseDTO.CurrentLearning.of(
                    progress.getLearning(), progress.getLearningStep().getTitle(),
                    progressRate, nextStepId);
        }

        return null;
    }

    // 최근 진행 기록 조회 — 최상단 항목의 패키지가 100%여도 더 훑을 수 있게 1건이 아니라 상위 20건을 가져온다
    private List<UserLearningProgress> findRecentActiveProgress(Long userId) {
        return userLearningProgressRepository
                .findTop20ByUser_UserIdAndLearning_IsActiveTrueOrderByLastStudiedAtDescIdDesc(userId);
    }

    // 추천 학습 폴백(인접 단계 보충)용 단건 조회 — 재도전 스캔이 필요 없어 기존 방식 유지
    private Optional<UserLearningProgress> findLatestActiveProgress(Long userId) {
        return userLearningProgressRepository
                .findFirstByUser_UserIdAndLearning_IsActiveTrueOrderByLastStudiedAtDescIdDesc(userId);
    }

    public List<LearningHomeResponseDTO.RecommendedLearning> getRecommendedLearnings(Long userId) {
        return getRecommendedLearnings(userId, null);
    }

    public List<LearningHomeResponseDTO.RecommendedLearning> getRecommendedLearnings(Long userId, Long excludeStepId) {
        List<Learning> orderedPackages = RECOMMENDATION_DIFFICULTY_ORDER.stream()
                .flatMap(difficulty -> learningRepository
                        .findFirstByCategoryAndDifficultyAndIsActiveTrueOrderByTitleAsc(LearningCategory.THEORY, difficulty)
                        .stream())
                .toList();

        List<LearningHomeResponseDTO.RecommendedLearning> incompleteCandidates =
                buildIncompleteRecommendations(userId, orderedPackages, excludeStepId);

        if (incompleteCandidates.size() >= RECOMMENDED_LEARNING_LIMIT) {
            return incompleteCandidates;
        }

        // 제외/완료로 인해 추천 후보가 부족하면 최근 학습 단계와 인접한 단계로 보충
        List<Long> excludedStepIds = new ArrayList<>(incompleteCandidates.stream()
                .map(LearningHomeResponseDTO.RecommendedLearning::nextStepId)
                .toList());
        if (excludeStepId != null) {
            excludedStepIds.add(excludeStepId);
        }
        int shortage = RECOMMENDED_LEARNING_LIMIT - incompleteCandidates.size();
        List<LearningHomeResponseDTO.RecommendedLearning> fallback =
                buildAdjacentFallbackRecommendations(userId, excludedStepIds, shortage);

        return Stream.concat(incompleteCandidates.stream(), fallback.stream()).toList();
    }

    private List<LearningHomeResponseDTO.RecommendedLearning> buildIncompleteRecommendations(
            Long userId, List<Learning> orderedPackages, Long excludeStepId) {
        if (orderedPackages.isEmpty()) {
            return List.of();
        }

        List<Long> learningIds = orderedPackages.stream().map(Learning::getId).toList();
        Map<Long, Learning> learningById = orderedPackages.stream()
                .collect(Collectors.toMap(Learning::getId, Function.identity()));

        List<LearningStep> orderedSteps = orderedPackages.stream()
                .flatMap(learning -> learningStepRepository.findByLearning_IdOrderByStepNoAsc(learning.getId()).stream())
                .toList();

        // null score toMap NPE 방지
        Map<Long, Integer> scoreByStepId = userLearningProgressRepository
                .findByUser_UserIdAndLearning_IdIn(userId, learningIds).stream()
                .filter(p -> p.getScore() != null)
                .collect(Collectors.toMap(p -> p.getLearningStep().getId(), UserLearningProgress::getScore));

        return orderedSteps.stream()
                .filter(step -> isStepIncomplete(scoreByStepId.get(step.getId())))
                .filter(step -> !step.getId().equals(excludeStepId))
                .limit(RECOMMENDED_LEARNING_LIMIT)
                .map(step -> LearningHomeResponseDTO.RecommendedLearning.of(learningById.get(step.getLearning().getId()), step))
                .toList();
    }

    private List<LearningHomeResponseDTO.RecommendedLearning> buildAdjacentFallbackRecommendations(
            Long userId, List<Long> excludedStepIds, int needed) {
        if (needed <= 0) {
            return List.of();
        }

        return findLatestActiveProgress(userId)
                .filter(latest -> latest.getLearning().getCategory() == LearningCategory.THEORY)
                .map(latest -> buildAdjacentStepRecommendations(latest, excludedStepIds, needed))
                .orElse(List.of());
    }

    // 완료 여부와 무관하게 최근 학습 단계와 인접한 단계로 보충(콘텐츠 노출 목적)
    private List<LearningHomeResponseDTO.RecommendedLearning> buildAdjacentStepRecommendations(
            UserLearningProgress latest, List<Long> excludedStepIds, int needed) {
        Learning learning = latest.getLearning();
        LearningStep referenceStep = latest.getLearningStep();
        List<LearningStep> steps = learningStepRepository.findByLearning_IdOrderByStepNoAsc(learning.getId());

        List<LearningStep> forward = steps.stream()
                .filter(step -> step.getStepNo() > referenceStep.getStepNo())
                .filter(step -> !excludedStepIds.contains(step.getId()))
                .toList();

        List<LearningStep> backward = new ArrayList<>(steps.stream()
                .filter(step -> step.getStepNo() < referenceStep.getStepNo())
                .filter(step -> !excludedStepIds.contains(step.getId()))
                .toList());
        Collections.reverse(backward);

        List<LearningStep> adjacentSteps = new ArrayList<>(forward.stream().limit(needed).toList());
        if (adjacentSteps.size() < needed) {
            adjacentSteps.addAll(backward.stream().limit(needed - adjacentSteps.size()).toList());
        }

        return adjacentSteps.stream()
                .map(step -> LearningHomeResponseDTO.RecommendedLearning.of(learning, step))
                .toList();
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
