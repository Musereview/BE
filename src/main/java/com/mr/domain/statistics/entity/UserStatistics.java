package com.mr.domain.statistics.entity;

import com.mr.domain.statistics.exception.StatisticsErrorStatus;
import com.mr.domain.user.entity.User;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_statistics")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStatistics extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_statistic_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "total_practice_minutes", nullable = false)
    private Integer totalPracticeMinutes;

    @Column(name = "total_practice_count", nullable = false)
    private Integer totalPracticeCount;

    @Column(name = "total_analysis_count", nullable = false)
    private Integer totalAnalysisCount;

    // 미집계 지표
    @Column(name = "average_score", precision = 5, scale = 2)
    private BigDecimal averageScore;

    // 전체 완료 분석 평균 점수
    @Column(name = "average_accuracy", precision = 5, scale = 2)
    private BigDecimal averageAccuracy;

    @Column(name = "recent_practiced_at")
    private Instant recentPracticedAt;

    private UserStatistics(User user, Integer totalPracticeMinutes, Integer totalPracticeCount,
                           Integer totalAnalysisCount, BigDecimal averageScore, BigDecimal averageAccuracy,
                           Instant recentPracticedAt) {
        validateUser(user);

        this.user = user;
        this.totalPracticeMinutes = totalPracticeMinutes;
        this.totalPracticeCount = totalPracticeCount;
        this.totalAnalysisCount = totalAnalysisCount;
        this.averageScore = averageScore;
        this.averageAccuracy = averageAccuracy;
        this.recentPracticedAt = recentPracticedAt;
    }

    private static void validateUser(User user) {
        if (user == null) {
            throw new GeneralException(StatisticsErrorStatus.STATISTICS_INVALID_REQUEST);
        }
    }

    public static UserStatistics createForUser(User user) {
        return new UserStatistics(
                user,
                0,
                0,
                0,
                null,
                null,
                null
        );
    }

    public void updatePracticeSummary(Integer totalPracticeMinutes, Integer totalPracticeCount,
                                      Instant recentPracticedAt) {
        this.totalPracticeMinutes = totalPracticeMinutes;
        this.totalPracticeCount = totalPracticeCount;
        this.recentPracticedAt = recentPracticedAt;
    }

    public void updateAnalysisSummary(Integer totalAnalysisCount, BigDecimal averageAccuracy) {
        this.totalAnalysisCount = totalAnalysisCount;
        this.averageAccuracy = averageAccuracy;
    }
}
