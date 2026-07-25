package com.mr.domain.analysis.repository;

import com.mr.domain.analysis.entity.AnalysisReport;
import com.mr.domain.analysis.entity.enums.LlmStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, Long> {

    Optional<AnalysisReport> findFirstByAnalysisIdAndLlmStatusOrderByCreatedAtDesc(
            Long analysisId,
            LlmStatus llmStatus
    );
}