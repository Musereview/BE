package com.mr.domain.analysis.service;

import com.mr.domain.analysis.event.AnalysisRequestedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class AnalysisRequestedEventListener {

    private final AnalysisProcessingService analysisProcessingService;
    private final TaskExecutor taskExecutor;

    public AnalysisRequestedEventListener(
            AnalysisProcessingService analysisProcessingService,
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor
    ) {
        this.analysisProcessingService = analysisProcessingService;
        this.taskExecutor = taskExecutor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AnalysisRequestedEvent event) {
        try {
            taskExecutor.execute(() -> analysisProcessingService.process(event.analysisId()));
        } catch (RuntimeException exception) {
            log.warn("AI analysis submission failed; it will be retried by recovery. analysisId={}",
                    event.analysisId(), exception);
        }
    }
}
