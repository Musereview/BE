package com.mr.domain.analysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.analysis.dto.res.AnalysisResultResponseDTO;
import com.mr.domain.analysis.dto.res.AnalysisStatusResponseDTO;
import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.AnalysisReport;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.entity.enums.LlmStatus;
import com.mr.domain.analysis.exception.AnalysisErrorStatus;
import com.mr.domain.analysis.repository.AnalysisReportRepository;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.global.apipayload.exception.GeneralException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final AnalysisReportRepository analysisReportRepository;
    private final ObjectMapper objectMapper;

    public AnalysisStatusResponseDTO getAnalysisStatus(
            Long userId,
            Long analysisId
    ) {
        Analysis analysis = getAnalysis(analysisId);

        validateOwner(analysis, userId);

        return AnalysisStatusResponseDTO.from(
                analysis,
                getProgressRate(analysis.getStatus()),
                getStatusMessage(analysis.getStatus())
        );
    }

    public AnalysisResultResponseDTO getAnalysisResult(
            Long userId,
            Long analysisId
    ) {
        Analysis analysis = getAnalysis(analysisId);

        validateOwner(analysis, userId);
        validateCompleted(analysis);

        // 정상 생성된 리포트만 반환한다. 실패/처리 중 리포트를 노출할지는 별도 정책 결정 필요 (#TODO).
        AnalysisReport analysisReport = analysisReportRepository
                .findFirstByAnalysisIdAndLlmStatusOrderByCreatedAtDesc(analysisId, LlmStatus.SUCCESS)
                .orElse(null);

        JsonNode rawResult = parseRawResult(
                analysis.getId(),
                analysis.getRawResultJson()
        );

        return AnalysisResultResponseDTO.from(
                analysis,
                analysisReport,
                rawResult
        );
    }

    private Analysis getAnalysis(Long analysisId) {
        return analysisRepository.findById(analysisId)
                .orElseThrow(() ->
                        new GeneralException(AnalysisErrorStatus.ANALYSIS_NOT_FOUND)
                );
    }

    private void validateOwner(Analysis analysis, Long userId) {
        if (!Objects.equals(analysis.getUserId(), userId)) {
            throw new GeneralException(AnalysisErrorStatus.ANALYSIS_ACCESS_DENIED);
        }
    }

    private void validateCompleted(Analysis analysis) {
        if (analysis.getStatus() != AnalysisStatus.COMPLETED) {
            throw new GeneralException(AnalysisErrorStatus.ANALYSIS_NOT_COMPLETED);
        }
    }

    private Integer getProgressRate(AnalysisStatus status) {
        return switch (status) {
            case PENDING -> 0;
            case PROCESSING -> null;
            case COMPLETED -> 100;
            case FAILED -> null;
        };
    }

    private String getStatusMessage(AnalysisStatus status) {
        return switch (status) {
            case PENDING -> "분석 요청을 기다리고 있습니다.";
            case PROCESSING -> "연습 결과를 분석 중입니다.";
            case COMPLETED -> "분석이 완료되었습니다.";
            case FAILED -> "분석에 실패했습니다.";
        };
    }

    private JsonNode parseRawResult(
            Long analysisId,
            String rawResultJson
    ) {
        if (rawResultJson == null || rawResultJson.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readTree(rawResultJson);
        } catch (JsonProcessingException exception) {
            log.error(
                    "분석 결과 JSON 변환 실패. analysisId={}",
                    analysisId,
                    exception
            );

            throw new GeneralException(
                    AnalysisErrorStatus.INVALID_RAW_RESULT
            );
        }
    }
}