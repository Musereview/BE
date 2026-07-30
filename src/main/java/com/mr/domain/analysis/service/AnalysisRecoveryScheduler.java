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
    private final int batchSize;

    public AnalysisRecoveryScheduler(
            AnalysisRepository analysisRepository,
            AnalysisProcessingService analysisProcessingService,
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
            @Value("${analysis.recovery.pending-threshold:1m}") Duration pendingThreshold,
            @Value("${analysis.recovery.batch-size:20}") int batchSize
    ) {
        this.analysisRepository = analysisRepository;
        this.analysisProcessingService = analysisProcessingService;
        this.taskExecutor = taskExecutor;
        this.pendingThreshold = pendingThreshold;
        this.batchSize = batchSize;
    }

    @Scheduled(
            initialDelayString = "${analysis.recovery.initial-delay-ms:10000}",
            fixedDelayString = "${analysis.recovery.fixed-delay-ms:30000}"
    )
    public void recoverPendingAnalyses() {
        LocalDateTime cutoff = LocalDateTime.now().minus(pendingThreshold);
        List<Long> analysisIds = analysisRepository.findIdsByStatusAndCreatedAtBefore(
                AnalysisStatus.PENDING,
                cutoff,
                PageRequest.of(0, batchSize)
        );

        for (Long analysisId : analysisIds) {
            try {
                taskExecutor.execute(() -> analysisProcessingService.process(analysisId));
            } catch (RuntimeException exception) {
                log.warn("Recovered AI analysis submission failed; it will be retried. analysisId={}",
                        analysisId, exception);
            }
        }
    }
}
