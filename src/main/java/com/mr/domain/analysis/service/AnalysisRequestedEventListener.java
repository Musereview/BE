package com.mr.domain.analysis.service;

import com.mr.domain.analysis.event.AnalysisRequestedEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AnalysisRequestedEventListener {

    private final AnalysisProcessingService analysisProcessingService;
    private final AnalysisStateService analysisStateService;
    private final TaskExecutor taskExecutor;

    public AnalysisRequestedEventListener(
            AnalysisProcessingService analysisProcessingService,
            AnalysisStateService analysisStateService,
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor
    ) {
        this.analysisProcessingService = analysisProcessingService;
        this.analysisStateService = analysisStateService;
        this.taskExecutor = taskExecutor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AnalysisRequestedEvent event) {
        try {
            taskExecutor.execute(() -> analysisProcessingService.process(event.analysisId()));
        } catch (RuntimeException exception) {
            analysisStateService.fail(event.analysisId(), exception.getMessage());
        }
    }
}
