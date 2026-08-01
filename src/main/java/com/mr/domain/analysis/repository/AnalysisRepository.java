package com.mr.domain.analysis.repository;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
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
            where a.status = :status and a.createdAt <= :cutoff
            order by a.createdAt asc, a.id asc
            """)
    List<Long> findIdsByStatusAndCreatedAtBefore(
            @Param("status") AnalysisStatus status,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable
    );

    @Query("""
            select a.id from Analysis a
            where a.status = :status and a.processingStartedAt <= :cutoff
            order by a.processingStartedAt asc, a.id asc
            """)
    List<Long> findIdsByStatusAndProcessingStartedAtBefore(
            @Param("status") AnalysisStatus status,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable
    );

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
            @Param("since") LocalDateTime since
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

    @Modifying
    @Query("delete from Analysis a where a.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
