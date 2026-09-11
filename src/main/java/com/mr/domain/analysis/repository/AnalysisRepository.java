package com.mr.domain.analysis.repository;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    boolean existsByPlayingIdAndStatusIn(Long playingId, List<AnalysisStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Analysis a where a.id = :analysisId")
    Optional<Analysis> findByIdForUpdate(@Param("analysisId") Long analysisId);

    @Query("""
            select a.id from Analysis a
            where a.status = com.mr.domain.analysis.entity.enums.AnalysisStatus.PENDING
              and a.createdAt <= :cutoff
            order by a.createdAt asc, a.id asc
            """)
    List<Long> findPendingIdsByCreatedAtBefore(
            @Param("cutoff") Instant cutoff,
            Pageable pageable
    );

    @Query("""
            select a.id from Analysis a
            where a.status = com.mr.domain.analysis.entity.enums.AnalysisStatus.PROCESSING
              and a.processingStartedAt <= :cutoff
            order by a.processingStartedAt asc, a.id asc
            """)
    List<Long> findProcessingIdsByProcessingStartedAtBefore(
            @Param("cutoff") Instant cutoff,
            Pageable pageable
    );

    @Query("select a.user.userId from Analysis a where a.id = :analysisId")
    Optional<Long> findUserIdById(@Param("analysisId") Long analysisId);

    @Query("""
            select a from Analysis a
            where a.playing.id in :playingIds and a.status = :status
            order by a.createdAt desc, a.id desc
            """)
    List<Analysis> findByPlayingIdInAndStatusOrderByCreatedAtDescIdDesc(
            @Param("playingIds") List<Long> playingIds, @Param("status") AnalysisStatus status);

    @Query("""
            select a from Analysis a
            where a.playing.id = :playingId and a.user.userId = :userId
            order by a.startBar asc, a.id asc
            """)
    List<Analysis> findByPlayingIdAndUserIdOrderByStartBarAscIdAsc(
            @Param("playingId") Long playingId, @Param("userId") Long userId);

    @Query("""
            select a from Analysis a
            where a.user.userId = :userId
              and a.status = :status
              and a.completedAt >= :since
            order by a.completedAt asc
            """)
    List<Analysis> findByUserAndStatusSince(
            @Param("userId") Long userId,
            @Param("status") AnalysisStatus status,
            @Param("since") Instant since
    );

    @Query("""
            select avg(a.totalScore)
            from Analysis a
            where a.user.userId = :userId
              and a.status = :status
              and a.completedAt >= :since
            """)
    Double aggregateAverageTotalScoreByUserAndStatusSince(
            @Param("userId") Long userId,
            @Param("status") AnalysisStatus status,
            @Param("since") Instant since
    );

    @Query("""
            select avg(a.scaleScore) as scaleScore,
                   avg(a.tensionScore) as tensionScore,
                   avg(a.progressionScore) as progressionScore,
                   avg(a.voiceLeadingScore) as voiceLeadingScore
            from Analysis a
            where a.user.userId = :userId
              and a.status = :status
              and a.completedAt >= :since
            """)
    WeeklySkillAverages aggregateWeeklySkillAveragesByUserAndStatusSince(
            @Param("userId") Long userId,
            @Param("status") AnalysisStatus status,
            @Param("since") Instant since
    );

    @Query("""
            select count(a) as analysisCount, avg(a.totalScore) as averageTotalScore
            from Analysis a
            where a.user.userId = :userId
              and a.status = :status
            """)
    AnalysisTotals aggregateTotalsByUserAndStatus(
            @Param("userId") Long userId,
            @Param("status") AnalysisStatus status
    );

    interface AnalysisTotals {
        Long getAnalysisCount();
        Double getAverageTotalScore();
    }

    interface WeeklySkillAverages {
        Double getScaleScore();
        Double getTensionScore();
        Double getProgressionScore();
        Double getVoiceLeadingScore();
    }

    @Modifying(clearAutomatically = true)
    @Query("delete from Analysis a where a.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
