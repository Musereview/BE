package com.mr.domain.learning.entity;

import com.mr.domain.learning.entity.enums.UserLearningStatus;
import com.mr.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "user_learning_progress"
)
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

    // 점수
    @Column(name = "score")
    private Integer score;

    // 마지막 학습일
    @Column(name = "last_studied_at")
    private LocalDateTime lastStudiedAt;

    // 학습 단계 완료 시 처리
    public void completeLearning(Integer score) {
        this.score = score;
        this.lastStudiedAt = LocalDateTime.now();
    }
}
