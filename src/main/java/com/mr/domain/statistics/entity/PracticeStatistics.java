package com.mr.domain.statistics.entity;

import com.mr.domain.statistics.entity.enums.PeriodType;
import com.mr.domain.statistics.exception.StatisticsErrorStatus;
import com.mr.domain.user.entity.User;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.entity.BaseCreatedEntity;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// TODO: (user, periodType, periodStart, periodEnd) 유니크 제약 필요한지 집계 로직 구현 시 확인
@Getter
@Entity
@Table(name = "practice_statistics")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PracticeStatistics extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "practice_statistics_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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

    private PracticeStatistics(User user, PeriodType periodType, LocalDate periodStart,
                               LocalDate periodEnd, Integer practiceMinutes, Integer sessionCount,
                               BigDecimal averageScore, BigDecimal averageAccuracy) {
        validateUser(user);

        this.user = user;
        this.periodType = periodType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.practiceMinutes = practiceMinutes;
        this.sessionCount = sessionCount;
        this.averageScore = averageScore;
        this.averageAccuracy = averageAccuracy;
    }

    private static void validateUser(User user) {
        if (user == null) {
            throw new GeneralException(StatisticsErrorStatus.STATISTICS_INVALID_REQUEST);
        }
    }

    public static PracticeStatistics create(User user, PeriodType periodType,
                                            LocalDate periodStart, LocalDate periodEnd) {
        return new PracticeStatistics(
                user,
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
