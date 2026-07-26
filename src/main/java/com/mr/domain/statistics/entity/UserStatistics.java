package com.mr.domain.statistics.entity;

import com.mr.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
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

    /** TODO: User 도메인 엔티티 연관관계 연결 예정 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_practice_minutes", nullable = false)
    private Integer totalPracticeMinutes;

    @Column(name = "total_practice_count", nullable = false)
    private Integer totalPracticeCount;

    @Column(name = "total_analysis_count", nullable = false)
    private Integer totalAnalysisCount;

    @Column(name = "average_score", precision = 5, scale = 2)
    private BigDecimal averageScore;

    @Column(name = "average_accuracy", precision = 5, scale = 2)
    private BigDecimal averageAccuracy;

    @Column(name = "recent_practiced_at")
    private LocalDateTime recentPracticedAt;

    private UserStatistics(Long userId, Integer totalPracticeMinutes, Integer totalPracticeCount,
                           Integer totalAnalysisCount, BigDecimal averageScore, BigDecimal averageAccuracy,
                           LocalDateTime recentPracticedAt) {
        this.userId = userId;
        this.totalPracticeMinutes = totalPracticeMinutes;
        this.totalPracticeCount = totalPracticeCount;
        this.totalAnalysisCount = totalAnalysisCount;
        this.averageScore = averageScore;
        this.averageAccuracy = averageAccuracy;
        this.recentPracticedAt = recentPracticedAt;
    }

    public static UserStatistics createForUser(Long userId) {
        return new UserStatistics(
                userId,
                0,
                0,
                0,
                null,
                null,
                null
        );
    }

    public void updatePracticeSummary(Integer totalPracticeMinutes, Integer totalPracticeCount,
                                      BigDecimal averageScore, BigDecimal averageAccuracy, LocalDateTime recentPracticedAt) {
        this.totalPracticeMinutes = totalPracticeMinutes;
        this.totalPracticeCount = totalPracticeCount;
        this.averageScore = averageScore;
        this.averageAccuracy = averageAccuracy;
        this.recentPracticedAt = recentPracticedAt;
    }

    public void updateAnalysisSummary(Integer totalAnalysisCount, BigDecimal averageScore) {
        this.totalAnalysisCount = totalAnalysisCount;
        this.averageScore = averageScore;
    }
}
