package com.mr.domain.analysis.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.analysis.exception.AnalysisErrorStatus;
import com.mr.domain.analysis.entity.enums.ReportGenerationType;
import com.mr.domain.analysis.generator.AnalysisResultEnricher;
import com.mr.domain.analysis.model.AnalysisProcessingClaim;
import com.mr.domain.analysis.model.GeneratedAnalysisReport;
import com.mr.domain.analysis.model.LlmCallMetadata;
import com.mr.domain.mentor.entity.enums.LlmCallStatus;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.client.ai.AiAnalysisRequest;
import com.mr.global.client.ai.AiServerClient;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    @Mock
    private ReportGenerationService reportGenerationService;

    @Test
    void process_doesNotCallAiWhenAnotherWorkerAlreadyClaimedAnalysis() {
        given(analysisStateService.startProcessing(1L)).willReturn(Optional.empty());
        AnalysisProcessingService service = new AnalysisProcessingService(
                analysisStateService, aiServerClient, reportGenerationService,
                new AnalysisResultEnricher(), new ObjectMapper()
        );

        service.process(1L);

        verify(aiServerClient, never()).requestAnalysis(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void recoverStaleProcessing_doesNotCallAiWhenWorkIsNoLongerStale() {
        Instant cutoff = Instant.now().minus(2, ChronoUnit.MINUTES);
        given(analysisStateService.restartStaleProcessing(1L, cutoff)).willReturn(Optional.empty());
        AnalysisProcessingService service = new AnalysisProcessingService(
                analysisStateService, aiServerClient, reportGenerationService,
                new AnalysisResultEnricher(), new ObjectMapper()
        );

        service.recoverStaleProcessing(1L, cutoff);

        verify(aiServerClient, never()).requestAnalysis(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void process_doesNotGenerateReportWhenAiResultIsInvalid() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode invalidResult = objectMapper.readTree("{}");
        Instant processingStartedAt = Instant.now();
        given(analysisStateService.startProcessing(1L))
                .willReturn(Optional.of(new AnalysisProcessingClaim(
                        "{\"meta\":null,\"chords\":[],\"notes\":[]}",
                        processingStartedAt
                )));
        given(aiServerClient.requestAnalysis(org.mockito.ArgumentMatchers.any(AiAnalysisRequest.class)))
                .willReturn(invalidResult);
        doThrow(new GeneralException(AnalysisErrorStatus.INVALID_RAW_RESULT))
                .when(analysisStateService).validateResult(invalidResult);
        AnalysisProcessingService service = new AnalysisProcessingService(
                analysisStateService, aiServerClient, reportGenerationService,
                new AnalysisResultEnricher(), objectMapper
        );

        service.process(1L);

        verify(reportGenerationService, never()).generate(org.mockito.ArgumentMatchers.any());
        verify(analysisStateService).fail(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(processingStartedAt),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void process_marksMalformedRequestAsFailedWithoutCallingAi() {
        Instant processingStartedAt = Instant.now();
        given(analysisStateService.startProcessing(1L))
                .willReturn(Optional.of(new AnalysisProcessingClaim("{invalid", processingStartedAt)));
        AnalysisProcessingService service = new AnalysisProcessingService(
                analysisStateService, aiServerClient, reportGenerationService,
                new AnalysisResultEnricher(), new ObjectMapper()
        );

        service.process(1L);

        verify(aiServerClient, never()).requestAnalysis(org.mockito.ArgumentMatchers.any());
        verify(analysisStateService).fail(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(processingStartedAt),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void process_passesGeneratedReportToFencedCompletion() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Instant processingStartedAt = Instant.now();
        JsonNode validResult = objectMapper.readTree("""
                {
                  "scores": {
                    "final_score": 80,
                    "domains": {
                      "스케일": 81,
                      "텐션": 82,
                      "진행": 83,
                      "코드 연결": 84
                    }
                  }
                }
                """);
        JsonNode enrichedResult = new AnalysisResultEnricher().enrich(validResult);
        GeneratedAnalysisReport generatedReport = new GeneratedAnalysisReport(
                ReportGenerationType.RULE_BASED,
                enrichedResult.path("summary").asText(),
                "리포트",
                "gemini-3-flash-preview",
                "analysis-report-v1",
                new LlmCallMetadata(
                        LlmCallStatus.FAILED,
                        "gemini-3-flash-preview",
                        "analysis-report-v1",
                        objectMapper.createObjectNode(),
                        null,
                        null,
                        null,
                        new BigDecimal("0.30"),
                        100,
                        false,
                        "input-hash",
                        "LLM 호출 실패"
                )
        );
        given(analysisStateService.startProcessing(1L))
                .willReturn(Optional.of(new AnalysisProcessingClaim(
                        "{\"meta\":null,\"chords\":[],\"notes\":[]}",
                        processingStartedAt
                )));
        given(aiServerClient.requestAnalysis(org.mockito.ArgumentMatchers.any(AiAnalysisRequest.class)))
                .willReturn(validResult);
        given(reportGenerationService.generate(validResult)).willReturn(generatedReport);
        given(analysisStateService.complete(
                1L,
                processingStartedAt,
                enrichedResult,
                enrichedResult.toString(),
                generatedReport
        )).willReturn(true);
        AnalysisProcessingService service = new AnalysisProcessingService(
                analysisStateService, aiServerClient, reportGenerationService,
                new AnalysisResultEnricher(), objectMapper
        );

        service.process(1L);

        verify(analysisStateService).validateResult(validResult);
        verify(reportGenerationService).generate(validResult);
        verify(analysisStateService).complete(
                1L,
                processingStartedAt,
                enrichedResult,
                enrichedResult.toString(),
                generatedReport
        );
        verify(analysisStateService, never()).fail(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }
}
