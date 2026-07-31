package com.mr.domain.analysis.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.AnalysisReport;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.entity.enums.ReportGenerationType;
import com.mr.domain.analysis.exception.AnalysisErrorStatus;
import com.mr.domain.analysis.model.GeneratedAnalysisReport;
import com.mr.domain.analysis.model.LlmCallMetadata;
import com.mr.domain.analysis.repository.AnalysisReportRepository;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.domain.mentor.entity.enums.LlmCallStatus;
import com.mr.domain.mentor.entity.LlmCallLog;
import com.mr.domain.mentor.repository.LlmCallLogRepository;
import com.mr.domain.user.entity.User;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.event.AnalysisCompletedEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AnalysisStateServiceTest {

    private static final LocalDateTime PROCESSING_STARTED_AT = LocalDateTime.of(2026, 7, 31, 12, 0);

    @Mock
    private AnalysisRepository analysisRepository;

    @Mock
    private AnalysisReportRepository analysisReportRepository;

    @Mock
    private LlmCallLogRepository llmCallLogRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AnalysisStateService service;
    private Analysis analysis;
    private User user;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        service = new AnalysisStateService(
                analysisRepository,
                analysisReportRepository,
                llmCallLogRepository,
                eventPublisher
        );
        analysis = mock(Analysis.class);
        user = mock(User.class);
        org.mockito.Mockito.lenient().when(user.getUserId()).thenReturn(1L);
        org.mockito.Mockito.lenient().when(analysis.getUser()).thenReturn(user);
        objectMapper = new ObjectMapper();
        org.mockito.Mockito.lenient()
                .when(analysisRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(analysis));
        org.mockito.Mockito.lenient()
                .when(analysis.isCurrentProcessing(PROCESSING_STARTED_AT))
                .thenReturn(true);
    }

    @Test
    void startProcessing_rejectsBlankRequestBeforeStateChange() {
        given(analysis.getStatus()).willReturn(AnalysisStatus.PENDING);
        given(analysis.getAnalysisRequestJson()).willReturn(" ");

        assertThatThrownBy(() -> service.startProcessing(1L))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", AnalysisErrorStatus.INVALID_ANALYSIS_REQUEST);
        verify(analysis, never()).startProcessing();
    }

    @Test
    void restartStaleProcessing_rejectsNullRequestBeforeStateChange() {
        LocalDateTime cutoff = LocalDateTime.now();
        given(analysis.getStatus()).willReturn(AnalysisStatus.PROCESSING);
        given(analysis.getProcessingStartedAt()).willReturn(cutoff.minusMinutes(1));
        given(analysis.getAnalysisRequestJson()).willReturn(null);

        assertThatThrownBy(() -> service.restartStaleProcessing(1L, cutoff))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", AnalysisErrorStatus.INVALID_ANALYSIS_REQUEST);
        verify(analysis, never()).restartProcessing();
    }

    @Test
    void complete_acceptsCompleteScoresWithinRange() throws Exception {
        JsonNode result = result("""
                {
                  "scores": {
                    "final_score": 80.5,
                    "grade": "GOOD",
                    "domains": {
                      "스케일": 81.0,
                      "텐션": 82.0,
                      "진행": 83.0,
                      "코드 연결": 84.0
                    }
                  },
                  "summary": "요약"
                }
                """);

        service.complete(1L, PROCESSING_STARTED_AT, result, result.toString(), generatedReport());

        verify(analysis).complete(
                81,
                com.mr.domain.analysis.entity.enums.AnalysisGrade.GOOD,
                "요약",
                new BigDecimal("81.0"),
                new BigDecimal("82.0"),
                new BigDecimal("83.0"),
                new BigDecimal("84.0"),
                result.toString()
        );
        verify(analysisReportRepository).save(any(AnalysisReport.class));
        verify(llmCallLogRepository).save(any(LlmCallLog.class));
        verify(eventPublisher).publishEvent(
                org.mockito.ArgumentMatchers.<Object>argThat(event ->
                        event instanceof AnalysisCompletedEvent completedEvent
                                && completedEvent.getUserId().equals(1L)
                )
        );
    }

    @Test
    void complete_persistsLlmReportAndSuccessfulCallLog() throws Exception {
        JsonNode result = validResult();

        service.complete(
                1L,
                PROCESSING_STARTED_AT,
                result,
                result.toString(),
                generatedReport(ReportGenerationType.LLM, LlmCallStatus.SUCCESS)
        );

        ArgumentCaptor<AnalysisReport> reportCaptor = ArgumentCaptor.forClass(AnalysisReport.class);
        ArgumentCaptor<LlmCallLog> logCaptor = ArgumentCaptor.forClass(LlmCallLog.class);
        verify(analysisReportRepository).save(reportCaptor.capture());
        verify(llmCallLogRepository).save(logCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(reportCaptor.getValue().getGenerationType())
                .isEqualTo(ReportGenerationType.LLM);
        org.assertj.core.api.Assertions.assertThat(logCaptor.getValue().getStatus())
                .isEqualTo(LlmCallStatus.SUCCESS);
        org.assertj.core.api.Assertions.assertThat(logCaptor.getValue().getTotalTokens()).isEqualTo(30);
    }

    @Test
    void complete_persistsTimeoutCallLogForFallbackReport() throws Exception {
        JsonNode result = validResult();

        service.complete(
                1L,
                PROCESSING_STARTED_AT,
                result,
                result.toString(),
                generatedReport(ReportGenerationType.RULE_BASED, LlmCallStatus.TIMEOUT)
        );

        ArgumentCaptor<LlmCallLog> logCaptor = ArgumentCaptor.forClass(LlmCallLog.class);
        verify(llmCallLogRepository).save(logCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(logCaptor.getValue().getStatus())
                .isEqualTo(LlmCallStatus.TIMEOUT);
        org.assertj.core.api.Assertions.assertThat(logCaptor.getValue().getErrorMessage())
                .isEqualTo("failed");
    }

    @Test
    void complete_ignoresStaleProcessingAttempt() throws Exception {
        JsonNode result = result("""
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
        given(analysis.isCurrentProcessing(PROCESSING_STARTED_AT)).willReturn(false);

        boolean completed = service.complete(
                1L, PROCESSING_STARTED_AT, result, result.toString(), generatedReport()
        );

        org.assertj.core.api.Assertions.assertThat(completed).isFalse();
        verify(analysis, never()).complete(any(), any(), any(), any(), any(), any(), any(), any());
        verify(analysisReportRepository, never()).save(any());
        verify(llmCallLogRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void fail_ignoresStaleProcessingAttempt() {
        given(analysis.isCurrentProcessing(PROCESSING_STARTED_AT)).willReturn(false);

        boolean failed = service.fail(1L, PROCESSING_STARTED_AT, "timeout");

        org.assertj.core.api.Assertions.assertThat(failed).isFalse();
        verify(analysis, never()).fail(any());
    }

    @Test
    void complete_rejectsMissingDomainScore() throws Exception {
        JsonNode result = result("""
                {
                  "scores": {
                    "final_score": 80,
                    "domains": {
                      "스케일": 81,
                      "텐션": 82,
                      "진행": 83
                    }
                  }
                }
                """);

        assertInvalidRawResult(result);
    }

    @Test
    void complete_rejectsScoreOutsideZeroToOneHundred() throws Exception {
        JsonNode result = result("""
                {
                  "scores": {
                    "final_score": 101,
                    "domains": {
                      "스케일": 81,
                      "텐션": 82,
                      "진행": 83,
                      "코드 연결": 84
                    }
                  }
                }
                """);

        assertInvalidRawResult(result);
    }

    @Test
    void complete_rejectsNonNumericScore() throws Exception {
        JsonNode result = result("""
                {
                  "scores": {
                    "final_score": 80,
                    "domains": {
                      "스케일": "81",
                      "텐션": 82,
                      "진행": 83,
                      "코드 연결": 84
                    }
                  }
                }
                """);

        assertInvalidRawResult(result);
    }

    private JsonNode result(String json) throws Exception {
        return objectMapper.readTree(json);
    }

    private JsonNode validResult() throws Exception {
        return result("""
                {
                  "scores": {
                    "final_score": 80,
                    "grade": "GOOD",
                    "domains": {
                      "스케일": 81,
                      "텐션": 82,
                      "진행": 83,
                      "코드 연결": 84
                    }
                  }
                }
                """);
    }

    private void assertInvalidRawResult(JsonNode result) {
        assertThatThrownBy(() -> service.complete(
                1L, PROCESSING_STARTED_AT, result, result.toString(), generatedReport()
        ))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", AnalysisErrorStatus.INVALID_RAW_RESULT);
        verify(analysis, never()).complete(any(), any(), any(), any(), any(), any(), any(), any());
    }

    private GeneratedAnalysisReport generatedReport() {
        return generatedReport(ReportGenerationType.RULE_BASED, LlmCallStatus.FAILED);
    }

    private GeneratedAnalysisReport generatedReport(
            ReportGenerationType generationType,
            LlmCallStatus status
    ) {
        return new GeneratedAnalysisReport(
                generationType,
                "리포트",
                "gemini-3-flash-preview",
                "analysis-report-v1",
                new LlmCallMetadata(
                        status,
                        "gemini-3-flash-preview",
                        "analysis-report-v1",
                        objectMapper.createObjectNode(),
                        10,
                        20,
                        30,
                        new BigDecimal("0.30"),
                        100,
                        false,
                        "hash",
                        "failed"
                )
        );
    }
}
