package com.mr.domain.weakness.entity;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.weakness.entity.enums.Severity;
import com.mr.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "weakness_note")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeaknessNote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weakness_note_id")
    private Long id;

    /** TODO: User 도메인 엔티티 연관관계 연결 예정 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "concept_code", nullable = false, length = 100)
    private String conceptCode;

    @Column(name = "concept_name", nullable = false, length = 100)
    private String conceptName;

    // TODO: WeaknessType 확정된 후 ENUM으로 변경
//    @Enumerated(EnumType.STRING)
//    @Column(name = "weakness_type", nullable = false, length = 30)
//    private WeaknessType weaknessType;
    @Column(name = "weakness_type", nullable = false, length = 30)
    private String weaknessType;

    @Column(name = "occurrence_count", nullable = false)
    private Integer occurrenceCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private Severity severity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_analysis_id")
    private Analysis lastAnalysis;

    /** TODO: Learning 도메인 엔티티 연관관계 연결 예정 */
    @Column(name = "recommended_learning_content_id")
    private Long recommendedLearningContentId;

    @Column(name = "last_detected_at")
    private LocalDateTime lastDetectedAt;

    private WeaknessNote(Long userId, String conceptCode, String conceptName, String weaknessType,
                         Integer occurrenceCount, Severity severity, Analysis lastAnalysis,
                         Long recommendedLearningContentId, LocalDateTime lastDetectedAt) {
        this.userId = userId;
        this.conceptCode = conceptCode;
        this.conceptName = conceptName;
        this.weaknessType = weaknessType;
        this.occurrenceCount = occurrenceCount;
        this.severity = severity;
        this.lastAnalysis = lastAnalysis;
        this.recommendedLearningContentId = recommendedLearningContentId;
        this.lastDetectedAt = lastDetectedAt;
    }

    public static WeaknessNote create(Long userId, String conceptCode, String conceptName,
                                      String weaknessType, Analysis lastAnalysis, Long recommendedLearningContentId) {
        return new WeaknessNote(
                userId,
                conceptCode,
                conceptName,
                weaknessType,
                1,
                Severity.LOW,
                lastAnalysis,
                recommendedLearningContentId,
                LocalDateTime.now()
        );
    }

    public void increaseOccurrence(Severity severity, Analysis lastAnalysis) {
        this.occurrenceCount += 1;
        this.severity = severity == null ? this.severity : severity;
        this.lastAnalysis = lastAnalysis;
        this.lastDetectedAt = LocalDateTime.now();
    }
}
