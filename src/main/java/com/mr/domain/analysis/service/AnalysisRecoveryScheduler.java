package com.mr.domain.analysis.service;

import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.repository.AnalysisRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AnalysisRecoveryScheduler {

    private final AnalysisRepository analysisRepository;
    private final AnalysisProcessingService analysisProcessingService;
    private final TaskExecutor taskExecutor;
    private final Duration pendingThreshold;
    private final Duration processingThreshold;
    private final int batchSize;

    public AnalysisRecoveryScheduler(
            AnalysisRepository analysisRepository,
            AnalysisProcessingService analysisProcessingService,
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
            @Value("${analysis.recovery.pending-threshold:1m}") Duration pendingThreshold,
            @Value("${analysis.recovery.processing-threshold:2m}") Duration processingThreshold,
            @Value("${analysis.recovery.batch-size:20}") int batchSize
    ) {
        this.analysisRepository = analysisRepository;
        this.analysisProcessingService = analysisProcessingService;
        this.taskExecutor = taskExecutor;
        this.pendingThreshold = pendingThreshold;
        this.processingThreshold = processingThreshold;
        this.batchSize = batchSize;
    }

    @Scheduled(
            initialDelayString = "${analysis.recovery.initial-delay-ms:10000}",
            fixedDelayString = "${analysis.recovery.fixed-delay-ms:30000}"
    )
    public void recoverPendingAnalyses() {
        LocalDateTime now = LocalDateTime.now();
        List<Long> pendingIds = analysisRepository.findIdsByStatusAndCreatedAtBefore(
                AnalysisStatus.PENDING,
                now.minus(pendingThreshold),
                PageRequest.of(0, batchSize)
        );
        submitPending(pendingIds);

        LocalDateTime processingCutoff = now.minus(processingThreshold);
        List<Long> processingIds = analysisRepository.findIdsByStatusAndProcessingStartedAtBefore(
                AnalysisStatus.PROCESSING,
                processingCutoff,
                PageRequest.of(0, batchSize)
        );
        submitStaleProcessing(processingIds, processingCutoff);
    }

    private void submitPending(List<Long> analysisIds) {
        for (Long analysisId : analysisIds) {
            try {
                taskExecutor.execute(() -> analysisProcessingService.process(analysisId));
            } catch (RuntimeException exception) {
                log.warn("Recovered AI analysis submission failed; it will be retried. analysisId={}",
                        analysisId, exception);
            }
        }
    }

    private void submitStaleProcessing(List<Long> analysisIds, LocalDateTime cutoff) {
        for (Long analysisId : analysisIds) {
            try {
                taskExecutor.execute(() -> analysisProcessingService.recoverStaleProcessing(analysisId, cutoff));
            } catch (RuntimeException exception) {
                log.warn("Stale AI analysis submission failed; it will be retried. analysisId={}",
                        analysisId, exception);
            }
        }
    }
}
