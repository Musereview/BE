package com.mr.domain.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.analysis.entity.enums.ReportGenerationType;
import com.mr.domain.analysis.generator.RuleBasedReportGenerator;
import com.mr.domain.analysis.model.GeneratedAnalysisReport;
import com.mr.global.client.gemini.GeminiClient;
import com.mr.global.client.gemini.GeminiGenerationResult;
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
                new RuleBasedReportGenerator(),
                new ObjectMapper()
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
        )).willReturn(new GeminiGenerationResult(
                validMarkdownReport(), 100, 50, 150, false
        ));

        GeneratedAnalysisReport report = service.generate(result);

        assertThat(report.generationType()).isEqualTo(ReportGenerationType.LLM);
        assertThat(report.content()).isEqualTo(validMarkdownReport());
        assertThat(report.modelName()).isEqualTo("gemini-3-flash-preview");
        assertThat(report.llmCall().promptTokens()).isEqualTo(100);
        assertThat(report.llmCall().totalTokens()).isEqualTo(150);
    }

    @Test
    void generate_fallsBackWhenGeminiResponseIsMissingRequiredHeading() {
        given(geminiClient.generateReport(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        )).willReturn(new GeminiGenerationResult(
                """
                        # 연주 분석 리포트
                        ## 총평
                        ## 잘한 점
                        ## 진행 맥락
                        ## 점수 요약
                        """,
                100, 50, 150, false
        ));

        GeneratedAnalysisReport report = service.generate(result);

        assertThat(report.generationType()).isEqualTo(ReportGenerationType.RULE_BASED);
        assertThat(report.content()).contains("# 연주 분석 리포트", "## 개선 제안", "80 / 100");
        assertThat(report.llmCall().status())
                .isEqualTo(com.mr.domain.mentor.entity.enums.LlmCallStatus.FAILED);
    }

    @Test
    void generate_fallsBackWhenGeminiHeadingsAreOutOfOrder() {
        given(geminiClient.generateReport(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        )).willReturn(new GeminiGenerationResult(
                """
                        # 연주 분석 리포트
                        ## 잘한 점
                        ## 총평
                        ## 진행 맥락
                        ## 개선 제안
                        ## 점수 요약
                        """,
                100, 50, 150, false
        ));

        GeneratedAnalysisReport report = service.generate(result);

        assertThat(report.generationType()).isEqualTo(ReportGenerationType.RULE_BASED);
        assertThat(report.content()).contains("# 연주 분석 리포트", "## 총평", "## 잘한 점");
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
        assertThat(report.llmCall().status())
                .isEqualTo(com.mr.domain.mentor.entity.enums.LlmCallStatus.FAILED);
    }

    private String validMarkdownReport() {
        return """
                # 연주 분석 리포트
                **조성** C major · **장르** jazz · **박자** 4/4 · **템포** 120 bpm
                ## 총평
                안정적인 연주입니다.
                ## 잘한 점
                스케일 선택이 좋습니다.
                ## 진행 맥락
                코드 진행을 잘 따랐습니다.
                ## 개선 제안
                텐션 활용을 연습하세요.
                ## 점수 요약
                종합 점수는 80점입니다.
                """;
    }
}
