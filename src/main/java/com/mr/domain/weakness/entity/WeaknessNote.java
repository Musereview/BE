package com.mr.domain.weakness.entity;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.learning.entity.Learning;
import com.mr.domain.user.entity.User;
import com.mr.domain.weakness.entity.enums.Severity;
import com.mr.domain.weakness.exception.WeaknessErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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

    // 아직 최근 분석이 없는 오답노트가 있을 수 있어 nullable, 검증 안 함
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_analysis_id")
    private Analysis lastAnalysis;

    // 매칭되는 추천 콘텐츠가 없을 수 있어 nullable, 검증 안 함
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_learning_content_id")
    private Learning recommendedLearningContent;

    @Column(name = "last_detected_at")
    private LocalDateTime lastDetectedAt;

    private WeaknessNote(User user, String conceptCode, String conceptName, String weaknessType,
                         Integer occurrenceCount, Severity severity, Analysis lastAnalysis,
                         Learning recommendedLearningContent, LocalDateTime lastDetectedAt) {
        validateUser(user);

        this.user = user;
        this.conceptCode = conceptCode;
        this.conceptName = conceptName;
        this.weaknessType = weaknessType;
        this.occurrenceCount = occurrenceCount;
        this.severity = severity;
        this.lastAnalysis = lastAnalysis;
        this.recommendedLearningContent = recommendedLearningContent;
        this.lastDetectedAt = lastDetectedAt;
    }

    private static void validateUser(User user) {
        if (user == null) {
            throw new GeneralException(WeaknessErrorStatus.WEAKNESS_INVALID_REQUEST);
        }
    }

    public static WeaknessNote create(User user, String conceptCode, String conceptName,
                                      String weaknessType, Analysis lastAnalysis, Learning recommendedLearningContent) {
        return new WeaknessNote(
                user,
                conceptCode,
                conceptName,
                weaknessType,
                1,
                Severity.LOW,
                lastAnalysis,
                recommendedLearningContent,
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
