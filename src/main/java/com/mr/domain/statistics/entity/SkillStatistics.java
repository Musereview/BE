package com.mr.domain.statistics.entity;

import com.mr.domain.statistics.entity.enums.PeriodType;
import com.mr.domain.statistics.entity.enums.SkillType;
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
@Table(name = "skill_statistics")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SkillStatistics extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skill_statistic_id")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_type", nullable = false, length = 50)
    private SkillType skillType;

    @Column(name = "score", nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "previous_score", precision = 5, scale = 2)
    private BigDecimal previousScore;

    private SkillStatistics(Long userId, PeriodType periodType, LocalDate periodStart,
                            LocalDate periodEnd, SkillType skillType, BigDecimal score, BigDecimal previousScore) {
        this.userId = userId;
        this.periodType = periodType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.skillType = skillType;
        this.score = score;
        this.previousScore = previousScore;
    }

    public static SkillStatistics create(Long userId, PeriodType periodType,
                                         LocalDate periodStart, LocalDate periodEnd, SkillType skillType, BigDecimal score) {
        return new SkillStatistics(
                userId,
                periodType,
                periodStart,
                periodEnd,
                skillType,
                score,
                null
        );
    }

    public static SkillStatistics createWithPreviousScore(Long userId, PeriodType periodType,
                                                          LocalDate periodStart, LocalDate periodEnd, SkillType skillType, BigDecimal score,
                                                          BigDecimal previousScore) {
        return new SkillStatistics(
                userId,
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
