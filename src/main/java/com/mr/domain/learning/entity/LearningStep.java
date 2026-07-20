package com.mr.domain.learning.entity;

import com.mr.global.entity.BaseCreatedEntity;
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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "learning_step",
        indexes = {
                @Index(name = "idx_learning_step_course_seq",
                        columnList = "learning_id, step_no")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LearningStep extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "learning_step_id")
    private Long id;

    // 학습 아이디
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_id", nullable = false)
    private Learning learning;

    // 순서
    @Column(name = "step_no", nullable = false)
    private Integer stepNo;

    // 제목
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    // 핵심 요약
    @Column(name = "summary", length = 255)
    private String summary;

    // 설명 / 이론 설명
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    // 내용 연습 팁
    @Column(name = "practice_tip", columnDefinition = "TEXT")
    private String practiceTip;

    // 소요 시간
    @Column(name = "estimated_minutes", nullable = false)
    private Integer estimatedMinutes = 10;
}
