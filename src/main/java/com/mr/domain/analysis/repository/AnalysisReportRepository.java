package com.mr.domain.analysis.repository;

import com.mr.domain.analysis.entity.AnalysisReport;
import com.mr.domain.analysis.entity.enums.LlmStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, Long> {

    Optional<AnalysisReport> findFirstByAnalysisIdAndLlmStatusOrderByCreatedAtDesc(
            Long analysisId,
            LlmStatus llmStatus
    );

    @Modifying
    @Query("delete from AnalysisReport ar where ar.analysis.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}