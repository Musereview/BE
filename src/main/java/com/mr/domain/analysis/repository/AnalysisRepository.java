package com.mr.domain.analysis.repository;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    List<Analysis> findByPlayingIdInAndStatusOrderByCreatedAtDescIdDesc(
            List<Long> playingIds, AnalysisStatus status);

    List<Analysis> findByPlayingIdAndUserIdOrderByStartBarAscIdAsc(
            Long playingId, Long userId);
}