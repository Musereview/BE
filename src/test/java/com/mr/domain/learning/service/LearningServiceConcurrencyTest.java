package com.mr.domain.learning.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mr.domain.learning.dto.req.LearningResultSaveRequestDTO;
import com.mr.domain.learning.entity.Learning;
import com.mr.domain.learning.entity.LearningStep;
import com.mr.domain.learning.entity.UserLearningProgress;
import com.mr.domain.learning.entity.enums.LearningCategory;
import com.mr.domain.learning.entity.enums.LearningDifficulty;
import com.mr.domain.learning.repository.LearningRepository;
import com.mr.domain.learning.repository.LearningStepRepository;
import com.mr.domain.learning.repository.UserLearningProgressRepository;
import com.mr.domain.notification.repository.NotificationRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.repository.UserRepository;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;

/**
 * 은우님 PR 리뷰 지적(#79) 검증용 통합 테스트.
 * 같은 패키지의 서로 다른 두 단계가 동시에 저장될 때 완료 알림이 정확히 1번만 발행되는지 확인한다.
 * 실제 트랜잭션 격리/락 동작을 봐야 해서 목이 아니라 실 DB를 쓰고,
 * 클래스 레벨 @Transactional을 안 걸어서(걸면 두 스레드가 같은 트랜잭션에 갇혀 테스트 의미가 없어짐) 각자 별도 트랜잭션으로 실행되게 한다.
 */
@SpringBootTest
class LearningServiceConcurrencyTest {

    @Autowired
    private LearningService learningService;
    @Autowired
    private LearningRepository learningRepository;
    @Autowired
    private LearningStepRepository learningStepRepository;
    @Autowired
    private UserLearningProgressRepository userLearningProgressRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    private Long userId;
    private Long learningId;

    @AfterEach
    void tearDown() {
        if (learningId != null) {
            userLearningProgressRepository.deleteAll(
                    userLearningProgressRepository.findByUser_UserIdAndLearning_Id(userId, learningId));
            learningStepRepository.findByLearning_IdOrderByStepNoAsc(learningId)
                    .forEach(step -> learningStepRepository.deleteById(step.getId()));
            learningRepository.deleteById(learningId);
        }
        if (userId != null) {
            notificationRepository.findAllByUser_UserIdAndDeletedAtIsNull(userId, Pageable.unpaged())
                    .forEach(n -> notificationRepository.deleteById(n.getId()));
            userRepository.deleteById(userId);
        }
    }

    @Test
    @DisplayName("같은 패키지의 서로 다른 두 단계를 동시에 완료해도 완료 알림은 정확히 1번만 발행된다")
    void concurrentSaveResultForDifferentSteps_publishesCompletionEventExactlyOnce() throws InterruptedException {
        // given: 3단계 패키지, 1단계는 이미 완료된 상태 → 나머지 2단계를 동시에 완료 처리
        User user = userRepository.save(User.createFromOAuth("https://cdn.example.com/profile/default.png"));
        userId = user.getUserId();

        Learning learning = learningRepository.save(Learning.create(
                "동시성 테스트 패키지", LearningCategory.THEORY, LearningDifficulty.BEGINNER,
                "요약", "본문", "팁", 10, "PIANO", true));
        learningId = learning.getId();

        LearningStep step1 = learningStepRepository.save(
                LearningStep.create(learning, 1, "1단계", "설명1", "본문1", "팁1", 10));
        LearningStep step2 = learningStepRepository.save(
                LearningStep.create(learning, 2, "2단계", "설명2", "본문2", "팁2", 10));
        LearningStep step3 = learningStepRepository.save(
                LearningStep.create(learning, 3, "3단계", "설명3", "본문3", "팁3", 10));

        UserLearningProgress step1Progress = UserLearningProgress.create(user, learning, step1);
        step1Progress.updateProgress(95, null);
        userLearningProgressRepository.save(step1Progress);

        // when: 2단계/3단계를 동시에 완료 저장
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Runnable saveStep2 = () -> {
            readyLatch.countDown();
            await(startLatch);
            learningService.saveResult(userId, learningId,
                    new LearningResultSaveRequestDTO.SaveResultDTO(95, step2.getId()));
        };
        Runnable saveStep3 = () -> {
            readyLatch.countDown();
            await(startLatch);
            learningService.saveResult(userId, learningId,
                    new LearningResultSaveRequestDTO.SaveResultDTO(95, step3.getId()));
        };

        executor.submit(saveStep2);
        executor.submit(saveStep3);
        readyLatch.await(5, TimeUnit.SECONDS); // 두 스레드 다 대기 지점에 도달할 때까지 대기
        startLatch.countDown(); // 동시에 출발
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        // then: 완료 알림(제목에 패키지명 포함)이 정확히 1건만 발행됐는지 확인 (after-commit 리스너 반영 대기 포함)
        List<com.mr.domain.notification.entity.Notification> notifications =
                waitForNotifications(userId, "동시성 테스트 패키지");

        assertThat(notifications).hasSize(1);
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    // NotificationEventListener가 AFTER_COMMIT 시점에 반영되므로 짧게 폴링
    private List<com.mr.domain.notification.entity.Notification> waitForNotifications(Long userId, String titleContains) {
        long deadline = System.currentTimeMillis() + 3000;
        List<com.mr.domain.notification.entity.Notification> result;
        do {
            result = notificationRepository.findAllByUser_UserIdAndDeletedAtIsNull(userId, Pageable.unpaged())
                    .getContent().stream()
                    .filter(n -> n.getTitle().contains(titleContains))
                    .toList();
            if (!result.isEmpty()) {
                break;
            }
            sleep(100);
        } while (System.currentTimeMillis() < deadline);
        return result;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
