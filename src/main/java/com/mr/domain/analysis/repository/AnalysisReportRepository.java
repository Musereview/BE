package com.mr.domain.analysis.repository;

import com.mr.domain.analysis.entity.AnalysisReport;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, Long> {

    Optional<AnalysisReport> findFirstByAnalysisIdOrderByCreatedAtDesc(
            Long analysisId
    );
}