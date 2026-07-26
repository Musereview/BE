package com.mr.domain.learning.entity;

import com.mr.domain.learning.exception.LearningErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
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
import lombok.Builder;

@Entity
@Getter
@Table(
        name = "learning_step",
        indexes = {
                @Index(name = "idx_learning_step_course_seq",
                        columnList = "learning_id, step_no",
                        unique = true)
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

    @Builder(access = AccessLevel.PRIVATE)
    private LearningStep(Learning learning, Integer stepNo, String title,
                         String summary, String content, String practiceTip,
                         Integer estimatedMinutes) {
        this.learning = learning;
        this.stepNo = stepNo;
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.practiceTip = practiceTip;
        this.estimatedMinutes = estimatedMinutes != null ? estimatedMinutes : 10;
    }

    public static LearningStep create(Learning learning, Integer stepNo, String title,
                                      String summary, String content, String practiceTip,
                                      Integer estimatedMinutes) {
        return LearningStep.builder()
                .learning(learning)
                .stepNo(stepNo)
                .title(title)
                .summary(summary)
                .content(content)
                .practiceTip(practiceTip)
                .estimatedMinutes(estimatedMinutes)
                .build();
    }

    public void updateStepInfo(Integer stepNo, String title, String summary,
                               String content, String practiceTip, Integer estimatedMinutes) {
        this.stepNo = stepNo;
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.practiceTip = practiceTip;
        if (estimatedMinutes != null) {
            this.estimatedMinutes = estimatedMinutes;
        }
    }

    public void validateBelongsTo(Learning targetLearning) {
        if (this.learning == null || !this.learning.getId().equals(targetLearning.getId())) {
            throw new GeneralException(LearningErrorStatus.INVALID_LEARNING_STEP);
        }
    }
}
