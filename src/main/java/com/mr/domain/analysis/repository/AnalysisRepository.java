package com.mr.domain.analysis.repository;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
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
}
