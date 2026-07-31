package com.mr.domain.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.analysis.generator.AnalysisResultEnricher;
import com.mr.domain.analysis.model.AnalysisProcessingClaim;
import com.mr.domain.analysis.model.GeneratedAnalysisReport;
import com.mr.global.client.ai.AiAnalysisRequest;
import com.mr.global.client.ai.AiServerClient;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisProcessingService {

    private final AnalysisStateService analysisStateService;
    private final AiServerClient aiServerClient;
    private final ReportGenerationService reportGenerationService;
    private final AnalysisResultEnricher analysisResultEnricher;
    private final ObjectMapper objectMapper;

    public void process(Long analysisId) {
        processClaimed(analysisId, () -> analysisStateService.startProcessing(analysisId));
    }

    public void recoverStaleProcessing(Long analysisId, LocalDateTime cutoff) {
        processClaimed(analysisId, () -> analysisStateService.restartStaleProcessing(analysisId, cutoff));
    }

    private void processClaimed(Long analysisId, Supplier<Optional<AnalysisProcessingClaim>> claim) {
        AnalysisProcessingClaim activeClaim = null;
        try {
            Optional<AnalysisProcessingClaim> claimed = claim.get();
            if (claimed.isEmpty()) {
                return;
            }
            activeClaim = claimed.get();
            AiAnalysisRequest request = objectMapper.readValue(activeClaim.requestJson(), AiAnalysisRequest.class);
            JsonNode result = aiServerClient.requestAnalysis(request);
            analysisStateService.validateResult(result);
            result = analysisResultEnricher.enrich(result);
            GeneratedAnalysisReport report = reportGenerationService.generate(result);
            boolean completed = analysisStateService.complete(
                    analysisId,
                    activeClaim.processingStartedAt(),
                    result,
                    objectMapper.writeValueAsString(result),
                    report
            );
            if (!completed) {
                log.info("Ignoring stale AI analysis completion. analysisId={}", analysisId);
            }
        } catch (Exception exception) {
            log.error("AI analysis failed. analysisId={}", analysisId, exception);
            if (activeClaim != null) {
                try {
                    boolean failed = analysisStateService.fail(
                            analysisId,
                            activeClaim.processingStartedAt(),
                            exception.getMessage()
                    );
                    if (!failed) {
                        log.info("Ignoring stale AI analysis failure. analysisId={}", analysisId);
                    }
                } catch (Exception failException) {
                    log.error("Failed to persist AI analysis failure. analysisId={}", analysisId, failException);
                }
            }
        }
    }
}
