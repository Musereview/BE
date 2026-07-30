package com.mr.domain.analysis.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.repository.AnalysisRepository;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AnalysisRecoverySchedulerTest {

    @Mock
    private AnalysisRepository analysisRepository;

    @Mock
    private AnalysisProcessingService analysisProcessingService;

    @Test
    void recoverPendingAnalyses_submitsPersistedPendingWork() {
        given(analysisRepository.findIdsByStatusAndCreatedAtBefore(
                eq(AnalysisStatus.PENDING), any(), any(Pageable.class)
        )).willReturn(List.of(11L, 12L));
        given(analysisRepository.findIdsByStatusAndProcessingStartedAtBefore(
                eq(AnalysisStatus.PROCESSING), any(), any(Pageable.class)
        )).willReturn(List.of());
        AnalysisRecoveryScheduler scheduler = new AnalysisRecoveryScheduler(
                analysisRepository,
                analysisProcessingService,
                new SyncTaskExecutor(),
                Duration.ofMinutes(1),
                Duration.ofMinutes(2),
                20
        );

        scheduler.recoverPendingAnalyses();

        verify(analysisProcessingService).process(11L);
        verify(analysisProcessingService).process(12L);
    }

    @Test
    void recoverPendingAnalyses_resubmitsOnlyPersistedStaleProcessingWork() {
        given(analysisRepository.findIdsByStatusAndCreatedAtBefore(
                eq(AnalysisStatus.PENDING), any(), any(Pageable.class)
        )).willReturn(List.of());
        given(analysisRepository.findIdsByStatusAndProcessingStartedAtBefore(
                eq(AnalysisStatus.PROCESSING), any(), any(Pageable.class)
        )).willReturn(List.of(21L));
        AnalysisRecoveryScheduler scheduler = new AnalysisRecoveryScheduler(
                analysisRepository,
                analysisProcessingService,
                new SyncTaskExecutor(),
                Duration.ofMinutes(1),
                Duration.ofMinutes(2),
                20
        );

        scheduler.recoverPendingAnalyses();

        verify(analysisProcessingService).recoverStaleProcessing(eq(21L), any());
    }
}
