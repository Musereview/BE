package com.mr.domain.learning.entity;

import com.mr.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "user_learning_progress",
        indexes = {
                // 1. 특정 유저가 이 학습 단계를 진행했는지 확인/조회할 때
                @Index(name = "idx_user_learning_step",
                        columnList = "user_id, learning_step_id"),

                // 2. 유저의 메인/대시보드 화면 등에서 최근 학습한 내역을 최신순으로 정렬해 보여줄 때
                @Index(name = "idx_user_last_studied",
                        columnList = "user_id, last_studied_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserLearingProgress extends BaseTimeEntity {

    @Id
    @Column(name = "user_learning_progress_id")
    private Long id;

    // 유저 아이디
    @Column(name = "user_id", nullable = false)
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
