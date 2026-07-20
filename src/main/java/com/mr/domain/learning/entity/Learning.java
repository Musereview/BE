package com.mr.domain.learning.entity;

import com.mr.domain.learning.entity.enums.LearningCategory;
import com.mr.domain.learning.entity.enums.LearningDifficulty;
import com.mr.global.entity.BaseTimeDeletedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "learning",
        indexes = {
                @Index(name = "idx_learning_active",
                        columnList = "is_active")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Learning extends BaseTimeDeletedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "learning_id")
    private Long id;

    //제목
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    // 카테고리
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private LearningCategory category;

    // 난이도
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 50)
    private LearningDifficulty difficulty;

    // 핵심 요약
    @Column(name = "summary", length = 255)
    private String summary;

    // 이론 설명
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    // 엽습 팁
    @Column(name = "practice_tip", columnDefinition = "TEXT")
    private String practiceTip;

    // 소요 시간
    @Column(name = "estimated_minutes")
    private int estimatedMinutes = 0;

    // 악기
    @Column(name = "instrument_type", nullable = false)
    private String instrumentType;

    // 활성 여부
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder(access = AccessLevel.PRIVATE)
    private Learning(String title, LearningCategory category, LearningDifficulty difficulty,
                     String summary, String content, String practiceTip,
                     Integer estimatedMinutes, String instrumentType, Boolean isActive) {
        this.title = title;
        this.category = category != null ? category : LearningCategory.THEORY;
        this.difficulty = difficulty != null ? difficulty : LearningDifficulty.BEGINNER; // Default: BEGINNER
        this.summary = summary;
        this.content = content;
        this.practiceTip = practiceTip;
        this.estimatedMinutes = estimatedMinutes;
        this.instrumentType = instrumentType;
        this.isActive = isActive != null ? isActive : true;
    }

    public static Learning create(String title, LearningCategory category, LearningDifficulty difficulty,
                                  String summary, String content, String practiceTip,
                                  Integer estimatedMinutes, String instrumentType, Boolean isActive) {
        return Learning.builder()
                .title(title)
                .category(category)
                .difficulty(difficulty)
                .summary(summary)
                .content(content)
                .practiceTip(practiceTip)
                .estimatedMinutes(estimatedMinutes)
                .instrumentType(instrumentType)
                .isActive(true)
                .build();
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updateContent(String title, LearningCategory category, LearningDifficulty difficulty,
                              String summary, String content, String practiceTip,
                              Integer estimatedMinutes, String instrumentType) {
        this.title = title;
        this.category = category;
        this.difficulty = difficulty;
        this.summary = summary;
        this.content = content;
        this.practiceTip = practiceTip;
        if (estimatedMinutes != null) this.estimatedMinutes = estimatedMinutes;
        if (instrumentType != null) this.instrumentType = instrumentType;
    }
}
