package com.mr.domain.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisGrade;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.exception.AnalysisErrorStatus;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.global.apipayload.exception.GeneralException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalysisStateService {

    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE = new BigDecimal("100");
    private static final String SCALE = "\uC2A4\uCF00\uC77C";
    private static final String TENSION = "\uD150\uC158";
    private static final String PROGRESSION = "\uC9C4\uD589";
    private static final String VOICE_LEADING = "\uCF54\uB4DC \uC5F0\uACB0";

    private final AnalysisRepository analysisRepository;

    @Transactional
    public Optional<String> startProcessing(Long analysisId) {
        Analysis analysis = analysisRepository.findByIdForUpdate(analysisId)
                .orElseThrow(() -> new GeneralException(AnalysisErrorStatus.ANALYSIS_NOT_FOUND));
        if (analysis.getStatus() != AnalysisStatus.PENDING) {
            return Optional.empty();
        }
        analysis.startProcessing();
        return Optional.of(analysis.getAnalysisRequestJson());
    }

    @Transactional
    public void complete(Long analysisId, JsonNode result, String rawResultJson) {
        Analysis analysis = getAnalysis(analysisId);
        if (result == null || !result.isObject()) {
            throw invalidRawResult();
        }
        JsonNode scores = result.path("scores");
        if (!scores.isObject()) {
            throw invalidRawResult();
        }
        BigDecimal finalScore = requiredScore(scores, "final_score");
        JsonNode domains = scores.path("domains");
        if (!domains.isObject()) {
            throw invalidRawResult();
        }
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
