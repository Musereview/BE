package com.mr.domain.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.global.client.ai.AiAnalysisRequest;
import com.mr.global.client.ai.AiServerClient;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisProcessingService {

    private final AnalysisStateService analysisStateService;
    private final AiServerClient aiServerClient;
    private final ObjectMapper objectMapper;

    public void process(Long analysisId) {
        boolean processingStarted = false;
        try {
            Optional<String> claimedRequest = analysisStateService.startProcessing(analysisId);
            if (claimedRequest.isEmpty()) {
                return;
            }
            processingStarted = true;
            String requestJson = claimedRequest.get();
            AiAnalysisRequest request = objectMapper.readValue(requestJson, AiAnalysisRequest.class);
            JsonNode result = aiServerClient.requestAnalysis(request);
            analysisStateService.complete(analysisId, result, objectMapper.writeValueAsString(result));
        } catch (Exception exception) {
            log.error("AI analysis failed. analysisId={}", analysisId, exception);
            if (processingStarted) {
                try {
                    analysisStateService.fail(analysisId, exception.getMessage());
                } catch (Exception failException) {
                    log.error("Failed to persist AI analysis failure. analysisId={}", analysisId, failException);
                }
            }
        }
    }
}
