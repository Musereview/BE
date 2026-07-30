package com.mr.domain.analysis.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.global.client.ai.AiServerClient;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalysisProcessingServiceTest {

    @Mock
    private AnalysisStateService analysisStateService;

    @Mock
    private AiServerClient aiServerClient;

    @Test
    void process_doesNotCallAiWhenAnotherWorkerAlreadyClaimedAnalysis() {
        given(analysisStateService.startProcessing(1L)).willReturn(Optional.empty());
        AnalysisProcessingService service = new AnalysisProcessingService(
                analysisStateService, aiServerClient, new ObjectMapper()
        );

        service.process(1L);

        verify(aiServerClient, never()).requestAnalysis(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void recoverStaleProcessing_doesNotCallAiWhenWorkIsNoLongerStale() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(2);
        given(analysisStateService.restartStaleProcessing(1L, cutoff)).willReturn(Optional.empty());
        AnalysisProcessingService service = new AnalysisProcessingService(
                analysisStateService, aiServerClient, new ObjectMapper()
        );

        service.recoverStaleProcessing(1L, cutoff);

        verify(aiServerClient, never()).requestAnalysis(org.mockito.ArgumentMatchers.any());
    }
}
