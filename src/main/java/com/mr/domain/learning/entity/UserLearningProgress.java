package com.mr.domain.learning.entity;

import com.mr.domain.learning.exception.LearningErrorStatus;
import com.mr.domain.user.entity.User;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Entity
@Getter
@Table(
        name = "user_learning_progress",
        indexes = {
                // 특정 유저가 이 학습 단계를 진행했는지 확인/조회할 때
                @Index(name = "idx_user_learning_step",
                        columnList = "user_id, learning_step_id",
                        unique = true),
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserLearningProgress extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_learning_progress_id")
    private Long id;

    // 유저 아이디
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 학습 아이디
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_id", nullable = false)
    private Learning learning;

    // 학습 단계 아이디
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_step_id", nullable = false)
    private LearningStep learningStep;

    // 점수
    @Column(name = "score")
    private Integer score;

    // 마지막 학습일
    @Column(name = "last_studied_at")
    private Instant lastStudiedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private UserLearningProgress(User user, Learning learning, LearningStep learningStep,
                                 Integer score, Instant lastStudiedAt) {
        this.user = user;
        this.learning = learning;
        this.learningStep = learningStep;
        this.score = score;
        this.lastStudiedAt = lastStudiedAt != null ? lastStudiedAt : Instant.now();
    }

    public static UserLearningProgress create(User user, Learning learning, LearningStep learningStep) {
        validateLearningAndStep(learning, learningStep);

        return UserLearningProgress.builder()
                .user(user)
                .learning(learning)
                .learningStep(learningStep)
                .lastStudiedAt(Instant.now())
                .build();
    }

    private static void validateLearningAndStep(Learning learning, LearningStep learningStep) {
        if (learningStep != null && learning != null) {
            if (learningStep.getLearning() == null ||
                    !Objects.equals(learningStep.getLearning().getId(), learning.getId())) {

                throw new GeneralException(LearningErrorStatus.INVALID_LEARNING_STEP);
            }
        }
    }

    // 학습 시간 및 점수 업데이트
    public void updateProgress(Integer newScore, Instant lastStudiedAt) {
        if (this.score != null && this.score >= 90) {
            if (newScore != null && newScore >= this.score) {   // 새로운 점수가 기존 점수 이상일 때만 갱신 (완료 -> 재도전 하락 방지)
                this.score = newScore;
            }
        }
        else {  // 기존 상태가 '미도전(null)'이거나 '재도전(90점 미만)'인 경우
            this.score = newScore;
        }
        this.lastStudiedAt = lastStudiedAt != null ? lastStudiedAt : Instant.now();
    }

    // 점수 기준 학습 진행 상태 파악
    public String getLearningStatus() {
        if (this.score == null) {
            return "NOT_STARTED";
        }
        return this.score >= 90 ? "COMPLETED" : "RETRY";
    }
}
