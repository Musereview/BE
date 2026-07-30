package com.mr.domain.analysis.repository;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

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

    // 통계 집계용 - completedAt 기준(Analysis.complete()에서 세팅되는 값)
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

    // 통계 집계용 전체 기간 요약 - row를 끌어오지 않고 단일 행으로 집계
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
}
