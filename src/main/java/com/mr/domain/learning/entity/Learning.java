package com.mr.domain.learning.entity;

import com.mr.domain.learning.entity.enums.LearningCategory;
import com.mr.domain.learning.entity.enums.LearningDifficulty;
import com.mr.global.entity.BaseTimeDeletedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name="learning")
public class Learning extends BaseTimeDeletedEntity {

    @Id
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

    // 활성 여부
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder(access = AccessLevel.PRIVATE)
    private Learning(String title, String subtitle, LearningCategory category, LearningDifficulty difficulty,
                     String summary, String content, String practiceTip, String diagramUrl, Boolean isActive) {
        this.title = title;
        this.category = category != null ? category : LearningCategory.THEORY;
        this.difficulty = difficulty != null ? difficulty : LearningDifficulty.BEGINNER; // Default: BEGINNER
        this.summary = summary;
        this.content = content;
        this.practiceTip = practiceTip;
        this.isActive = isActive != null ? isActive : true;
    }

    public static Learning create(String title, String subtitle, LearningCategory category, LearningDifficulty difficulty,
                                  String summary, String content, String practiceTip, String diagramUrl) {
        return Learning.builder()
                .title(title)
                .subtitle(subtitle)
                .category(category)
                .difficulty(difficulty)
                .summary(summary)
                .content(content)
                .practiceTip(practiceTip)
                .diagramUrl(diagramUrl)
                .isActive(true)
                .build();
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updateContent(String title, String subtitle, LearningCategory category, LearningDifficulty difficulty,
                              String summary, String content, String practiceTip, String diagramUrl) {
        this.title = title;
        this.category = category;
        this.difficulty = difficulty;
        this.summary = summary;
        this.content = content;
        this.practiceTip = practiceTip;
    }
}
