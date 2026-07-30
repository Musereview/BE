package com.mr.domain.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.analysis.entity.enums.ReportGenerationType;
import com.mr.global.client.gemini.GeminiClient;
import com.mr.global.config.GeminiProperties;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportGenerationServiceTest {

    @Mock
    private GeminiClient geminiClient;

    private ReportGenerationService service;
    private JsonNode result;

    @BeforeEach
    void setUp() throws Exception {
        GeminiProperties properties = new GeminiProperties(
                "https://generativelanguage.googleapis.com",
                "key",
                "gemini-3-flash-preview",
                Duration.ofSeconds(5),
                Duration.ofSeconds(60)
        );
        service = new ReportGenerationService(
                geminiClient,
                properties,
                new RuleBasedReportGenerator()
        );
        result = new ObjectMapper().readTree("""
                {
                  "meta": {"key":"C major","genre":"jazz","time_signature":[4,4],"bpm":120},
                  "scores": {
                    "final_score":80,
                    "domains":{"스케일":90,"텐션":70,"진행":85,"코드 연결":75}
                  }
                }
                """);
    }

    @Test
    void generate_returnsLlmReportWhenGeminiSucceeds() {
        given(geminiClient.generateReport(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        )).willReturn("# 생성된 리포트");

        GeneratedAnalysisReport report = service.generate(result);

        assertThat(report.generationType()).isEqualTo(ReportGenerationType.LLM);
        assertThat(report.content()).isEqualTo("# 생성된 리포트");
        assertThat(report.modelName()).isEqualTo("gemini-3-flash-preview");
    }

    @Test
    void generate_fallsBackToRuleBasedReportWhenGeminiFails() {
        given(geminiClient.generateReport(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        )).willThrow(new RuntimeException("quota exceeded"));

        GeneratedAnalysisReport report = service.generate(result);

        assertThat(report.generationType()).isEqualTo(ReportGenerationType.RULE_BASED);
        assertThat(report.content()).contains("# 연주 분석 리포트", "## 개선 제안", "80 / 100");
        assertThat(report.modelName()).isNull();
    }
}
