package com.mr.domain.analysis.repository;

import com.mr.domain.analysis.entity.Analysis;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    Optional<Analysis> findByIdAndUserId(Long analysisId, Long userId);
}