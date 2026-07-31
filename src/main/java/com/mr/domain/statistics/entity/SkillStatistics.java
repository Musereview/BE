package com.mr.domain.statistics.entity;

import com.mr.domain.statistics.entity.enums.PeriodType;
import com.mr.domain.statistics.entity.enums.SkillType;
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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "skill_statistics",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_skill_statistics_user_period_skill",
                        columnNames = {"user_id", "period_type", "period_start", "period_end", "skill_type"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SkillStatistics extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skill_statistic_id")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_type", nullable = false, length = 50)
    private SkillType skillType;

    @Column(name = "score", nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "previous_score", precision = 5, scale = 2)
    private BigDecimal previousScore;

    private SkillStatistics(User user, PeriodType periodType, LocalDate periodStart,
                            LocalDate periodEnd, SkillType skillType, BigDecimal score, BigDecimal previousScore) {
        validateUser(user);

        this.user = user;
        this.periodType = periodType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.skillType = skillType;
        this.score = score;
        this.previousScore = previousScore;
    }

    private static void validateUser(User user) {
        if (user == null) {
            throw new GeneralException(StatisticsErrorStatus.STATISTICS_INVALID_REQUEST);
        }
    }

    public static SkillStatistics create(User user, PeriodType periodType,
                                         LocalDate periodStart, LocalDate periodEnd, SkillType skillType, BigDecimal score) {
        return new SkillStatistics(
                user,
                periodType,
                periodStart,
                periodEnd,
                skillType,
                score,
                null
        );
    }

    public static SkillStatistics createWithPreviousScore(User user, PeriodType periodType,
                                                          LocalDate periodStart, LocalDate periodEnd, SkillType skillType, BigDecimal score,
                                                          BigDecimal previousScore) {
        return new SkillStatistics(
                user,
                periodType,
                periodStart,
                periodEnd,
                skillType,
                score,
                previousScore
        );
    }

    public void updateScore(BigDecimal score, BigDecimal previousScore) {
        this.score = score;
        this.previousScore = previousScore;
    }
}
