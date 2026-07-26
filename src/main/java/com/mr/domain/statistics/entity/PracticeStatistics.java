package com.mr.domain.statistics.entity;

import com.mr.domain.statistics.entity.enums.PeriodType;
import com.mr.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "practice_statistics")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PracticeStatistics extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "practice_statistics_id")
    private Long id;

    /** TODO: User 도메인 엔티티 연관관계 연결 예정 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 50)
    private PeriodType periodType;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "practice_minutes", nullable = false)
    private Integer practiceMinutes;

    @Column(name = "session_count", nullable = false)
    private Integer sessionCount;

    @Column(name = "average_score", precision = 5, scale = 2)
    private BigDecimal averageScore;

    @Column(name = "average_accuracy", precision = 5, scale = 2)
    private BigDecimal averageAccuracy;

    private PracticeStatistics(Long userId, PeriodType periodType, LocalDate periodStart,
                               LocalDate periodEnd, Integer practiceMinutes, Integer sessionCount,
                               BigDecimal averageScore, BigDecimal averageAccuracy) {
        this.userId = userId;
        this.periodType = periodType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.practiceMinutes = practiceMinutes;
        this.sessionCount = sessionCount;
        this.averageScore = averageScore;
        this.averageAccuracy = averageAccuracy;
    }

    public static PracticeStatistics create(Long userId, PeriodType periodType,
                                            LocalDate periodStart, LocalDate periodEnd) {
        return new PracticeStatistics(
                userId,
                periodType,
                periodStart,
                periodEnd,
                0,
                0,
                null,
                null
        );
    }

    public void update(Integer practiceMinutes, Integer sessionCount,
                       BigDecimal averageScore, BigDecimal averageAccuracy) {
        this.practiceMinutes = practiceMinutes;
        this.sessionCount = sessionCount;
        this.averageScore = averageScore;
        this.averageAccuracy = averageAccuracy;
    }
}
