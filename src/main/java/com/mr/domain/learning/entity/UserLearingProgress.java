package com.mr.domain.learning.entity;

import com.mr.domain.learning.entity.enums.UserLearningStatus;
import com.mr.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name="user_learning_progress")
public class UserLearingProgress extends BaseTimeEntity {

    @Id
    @Column(name = "user_learning_progress_id")
    private Long id;

    // 유저 아이디
    @JoinColumn(name = "user_id", nullable = false)
    private Long userId;

    // 학습 아이디
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_id", nullable = false)
    private Learning learning;

    // 학습 단계 아이디
    @JoinColumn(name = "learning_step_id", nullable = false)
    private Long learningStepId;

    // 진행률
    @Column(name = "progress_rate", nullable = false)
    private Integer progressRate;

    // 학습 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserLearningStatus status;

    // 점수
    @Column(name = "score")
    private Integer score;

    // 마지막 학습일
    @Column(name = "last_studied_at")
    private LocalDateTime lastStudiedAt;

    // 완료일
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // 
    public void startOrUpdateProgress(Integer progressRate, Integer score) {
        if (progressRate < 0 || progressRate > 100) {
            throw new IllegalArgumentException("진행률(progressRate)은 0에서 100 사이여야 합니다.");
        }

        // 아직 시작 안 한 상태에서 업데이트가 오면 IN_PROGRESS로 변경
        if (this.status == UserLearningStatus.NOT_STARTED) {
            this.status = UserLearningStatus.IN_PROGRESS;
        }

        this.progressRate = progressRate;
        this.score = score;
        this.lastStudiedAt = LocalDateTime.now(); // 마지막 학습일 갱신
    }

    // 학습 완료 처리
    public void completeLearning(Integer score) {
        this.status = UserLearningStatus.COMPLETED;
        this.progressRate = 100; // 완료 시 진행률 100% 강제
        this.score = score;
        this.lastStudiedAt = LocalDateTime.now();
        this.completedAt = LocalDateTime.now(); // 완료일 세팅
    }
}
