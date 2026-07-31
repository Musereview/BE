package com.mr.domain.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.AnalysisReport;
import com.mr.domain.analysis.entity.enums.AnalysisGrade;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.entity.enums.ReportGenerationType;
import com.mr.domain.analysis.exception.AnalysisErrorStatus;
import com.mr.domain.analysis.model.GeneratedAnalysisReport;
import com.mr.domain.analysis.model.LlmCallMetadata;
import com.mr.domain.analysis.repository.AnalysisReportRepository;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.domain.mentor.entity.LlmCallLog;
import com.mr.domain.mentor.entity.enums.LlmCallStatus;
import com.mr.domain.mentor.entity.enums.LlmPurpose;
import com.mr.domain.mentor.repository.LlmCallLogRepository;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.event.AnalysisCompletedEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalysisStateService {

    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE = new BigDecimal("100");
    private static final String SCALE = "스케일";
    private static final String TENSION = "텐션";
    private static final String PROGRESSION = "진행";
    private static final String VOICE_LEADING = "코드 연결";

    private final AnalysisRepository analysisRepository;
    private final AnalysisReportRepository analysisReportRepository;
    private final LlmCallLogRepository llmCallLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Optional<String> startProcessing(Long analysisId) {
        Analysis analysis = analysisRepository.findByIdForUpdate(analysisId)
                .orElseThrow(() -> new GeneralException(AnalysisErrorStatus.ANALYSIS_NOT_FOUND));
        if (analysis.getStatus() != AnalysisStatus.PENDING) {
            return Optional.empty();
        }
        String requestJson = requireRequestJson(analysis);
        analysis.startProcessing();
        return Optional.of(requestJson);
    }

    @Transactional
    public Optional<String> restartStaleProcessing(Long analysisId, LocalDateTime cutoff) {
        Analysis analysis = analysisRepository.findByIdForUpdate(analysisId)
                .orElseThrow(() -> new GeneralException(AnalysisErrorStatus.ANALYSIS_NOT_FOUND));
        if (analysis.getStatus() != AnalysisStatus.PROCESSING
                || analysis.getProcessingStartedAt() == null
                || analysis.getProcessingStartedAt().isAfter(cutoff)) {
            return Optional.empty();
        }
        String requestJson = requireRequestJson(analysis);
        analysis.restartProcessing();
        return Optional.of(requestJson);
    }

    @Transactional
    public void complete(
            Long analysisId,
            JsonNode result,
            String rawResultJson,
            GeneratedAnalysisReport generatedReport
    ) {
        Analysis analysis = getAnalysis(analysisId);
        validateResult(result);
        JsonNode scores = result.path("scores");
        BigDecimal finalScore = requiredScore(scores, "final_score");
        JsonNode domains = scores.path("domains");
        analysis.complete(
                finalScore.setScale(0, RoundingMode.HALF_UP).intValueExact(),
                resolveGrade(scores.path("grade").asText(), finalScore),
                result.path("summary").asText(null),
                requiredScore(domains, SCALE),
                requiredScore(domains, TENSION),
                requiredScore(domains, PROGRESSION),
                requiredScore(domains, VOICE_LEADING),
                rawResultJson
        );
        AnalysisReport report = generatedReport.generationType() == ReportGenerationType.LLM
                ? AnalysisReport.createLlmReport(
                        analysis,
                        generatedReport.content(),
                        generatedReport.modelName(),
                        generatedReport.promptVersion()
                )
                : AnalysisReport.createRuleBasedReport(analysis, generatedReport.content());
        analysisReportRepository.save(report);
        saveLlmCallLog(analysis, report, generatedReport.llmCall());
        eventPublisher.publishEvent(
                AnalysisCompletedEvent.of(analysis.getUser().getUserId())
        );
    }

    public void validateResult(JsonNode result) {
        if (result == null || !result.isObject()) {
            throw invalidRawResult();
        }
        JsonNode scores = result.path("scores");
        if (!scores.isObject()) {
            throw invalidRawResult();
        }
        requiredScore(scores, "final_score");
        JsonNode domains = scores.path("domains");
        if (!domains.isObject()) {
            throw invalidRawResult();
        }
        requiredScore(domains, SCALE);
        requiredScore(domains, TENSION);
        requiredScore(domains, PROGRESSION);
        requiredScore(domains, VOICE_LEADING);
    }

    private String requireRequestJson(Analysis analysis) {
        String requestJson = analysis.getAnalysisRequestJson();
        if (requestJson == null || requestJson.isBlank()) {
            throw new GeneralException(AnalysisErrorStatus.INVALID_ANALYSIS_REQUEST);
        }
        return requestJson;
    }

    private void saveLlmCallLog(
            Analysis analysis,
            AnalysisReport report,
            LlmCallMetadata metadata
    ) {
        LlmCallLog log = metadata.status() == LlmCallStatus.SUCCESS
                ? LlmCallLog.success(
                        analysis.getUser(), analysis, report, null,
                        LlmPurpose.REPORT_GENERATION, metadata.modelName(), metadata.promptVersion(),
                        metadata.promptSnapshot(), metadata.promptTokens(), metadata.completionTokens(),
                        metadata.totalTokens(), metadata.temperature(), metadata.latencyMs(),
                        metadata.cacheHit(), metadata.inputHash()
                )
                : metadata.status() == LlmCallStatus.TIMEOUT
                        ? LlmCallLog.timeout(
                                analysis.getUser(), analysis, report, null,
                                LlmPurpose.REPORT_GENERATION, metadata.modelName(), metadata.promptVersion(),
                                metadata.promptSnapshot(), metadata.temperature(), metadata.latencyMs(),
                                metadata.inputHash(), metadata.errorMessage()
                        )
                        : LlmCallLog.failed(
                                analysis.getUser(), analysis, report, null,
                                LlmPurpose.REPORT_GENERATION, metadata.modelName(), metadata.promptVersion(),
                                metadata.promptSnapshot(), metadata.temperature(), metadata.latencyMs(),
                                metadata.inputHash(), metadata.errorMessage()
                        );
        llmCallLogRepository.save(log);
    }

    @Transactional
    public void fail(Long analysisId, String reason) {
        Analysis analysis = getAnalysis(analysisId);
        analysis.fail(reason);
    }

    private Analysis getAnalysis(Long analysisId) {
        return analysisRepository.findById(analysisId)
                .orElseThrow(() -> new GeneralException(AnalysisErrorStatus.ANALYSIS_NOT_FOUND));
    }

    private BigDecimal requiredScore(JsonNode parent, String fieldName) {
        JsonNode node = parent.path(fieldName);
        if (!node.isNumber()) {
            throw invalidRawResult();
        }
        BigDecimal score = node.decimalValue();
        if (score.compareTo(MIN_SCORE) < 0 || score.compareTo(MAX_SCORE) > 0) {
            throw invalidRawResult();
        }
        return score;
    }

    private GeneralException invalidRawResult() {
        return new GeneralException(AnalysisErrorStatus.INVALID_RAW_RESULT);
    }

    private AnalysisGrade resolveGrade(String value, BigDecimal score) {
        return switch (value) {
            case "훌륭함", "EXCELLENT" -> AnalysisGrade.EXCELLENT;
            case "좋음", "GOOD" -> AnalysisGrade.GOOD;
            case "보통", "FAIR" -> AnalysisGrade.FAIR;
            case "연습 필요", "POOR" -> AnalysisGrade.POOR;
            default -> {
                if (score.compareTo(new BigDecimal("90")) >= 0) yield AnalysisGrade.EXCELLENT;
                if (score.compareTo(new BigDecimal("75")) >= 0) yield AnalysisGrade.GOOD;
                if (score.compareTo(new BigDecimal("60")) >= 0) yield AnalysisGrade.FAIR;
                yield AnalysisGrade.POOR;
            }
        };
    }
}
