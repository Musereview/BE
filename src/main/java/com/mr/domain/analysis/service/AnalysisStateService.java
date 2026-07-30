package com.mr.domain.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisGrade;
import com.mr.domain.analysis.exception.AnalysisErrorStatus;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.global.apipayload.exception.GeneralException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalysisStateService {

    private final AnalysisRepository analysisRepository;

    @Transactional
    public String startProcessing(Long analysisId) {
        Analysis analysis = getAnalysis(analysisId);
        analysis.startProcessing();
        return analysis.getAnalysisRequestJson();
    }

    @Transactional
    public void complete(Long analysisId, JsonNode result, String rawResultJson) {
        Analysis analysis = getAnalysis(analysisId);
        JsonNode scores = result.path("scores");
        JsonNode finalScoreNode = scores.path("final_score");
        if (!finalScoreNode.isNumber()) {
            throw new GeneralException(AnalysisErrorStatus.INVALID_RAW_RESULT);
        }
        BigDecimal finalScore = finalScoreNode.decimalValue();
        JsonNode domains = scores.path("domains");
        analysis.complete(
                finalScore.setScale(0, RoundingMode.HALF_UP).intValueExact(),
                resolveGrade(scores.path("grade").asText(), finalScore),
                result.path("summary").asText(null),
                decimal(domains.path("\uC2A4\uCF00\uC77C")),
                decimal(domains.path("\uD150\uC158")),
                decimal(domains.path("\uC9C4\uD589")),
                decimal(domains.path("\uCF54\uB4DC \uC5F0\uACB0")),
                rawResultJson
        );
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

    private BigDecimal decimal(JsonNode node) {
        return node.isNumber() ? node.decimalValue() : BigDecimal.ZERO;
    }

    private AnalysisGrade resolveGrade(String value, BigDecimal score) {
        return switch (value) {
            case "\uD6CC\uB96D\uD568", "EXCELLENT" -> AnalysisGrade.EXCELLENT;
            case "\uC88B\uC74C", "GOOD" -> AnalysisGrade.GOOD;
            case "\uBCF4\uD1B5", "FAIR" -> AnalysisGrade.FAIR;
            case "\uC5F0\uC2B5 \uD544\uC694", "POOR" -> AnalysisGrade.POOR;
            default -> {
                if (score.compareTo(new BigDecimal("90")) >= 0) yield AnalysisGrade.EXCELLENT;
                if (score.compareTo(new BigDecimal("75")) >= 0) yield AnalysisGrade.GOOD;
                if (score.compareTo(new BigDecimal("60")) >= 0) yield AnalysisGrade.FAIR;
                yield AnalysisGrade.POOR;
            }
        };
    }
}
