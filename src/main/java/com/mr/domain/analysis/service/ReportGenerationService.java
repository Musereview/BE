package com.mr.domain.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.analysis.entity.enums.ReportGenerationType;
import com.mr.domain.analysis.generator.RuleBasedReportGenerator;
import com.mr.domain.analysis.model.GeneratedAnalysisReport;
import com.mr.domain.analysis.model.LlmCallMetadata;
import com.mr.domain.mentor.entity.enums.LlmCallStatus;
import com.mr.global.client.gemini.GeminiClient;
import com.mr.global.client.gemini.GeminiGenerationResult;
import com.mr.global.config.GeminiProperties;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    static final String PROMPT_VERSION = "analysis-report-v2";
    private static final BigDecimal TEMPERATURE = new BigDecimal("0.30");
    private static final int MIN_REPORT_LENGTH = 600;
    private static final List<String> REQUIRED_HEADINGS = List.of(
            "# 연주 분석 리포트",
            "## 총평",
            "## 잘한 점",
            "## 진행 맥락",
            "## 개선 제안",
            "## 점수 요약"
    );
    private static final String SYSTEM_PROMPT = """
            당신은 재즈 화성학과 MIDI 연주 분석에 능숙한 친절한 음악 코치입니다.
            입력은 MuseReview 분석 서버가 생성한 JSON입니다. JSON에 존재하는 사실만 사용하세요.
            응답은 한국어 Markdown으로 작성하고 반드시 다음 순서를 지키세요.

            # 연주 분석 리포트
            **조성** ... · **장르** ... · **박자** ... · **템포** ... bpm
            ## 총평
            ## 잘한 점
            ## 진행 맥락
            ## 개선 제안
            ## 점수 요약

            섹션별 작성 기준은 다음과 같습니다.
            - 총평: 입력의 summary와 종합 점수를 바탕으로 3~4문장, 최소 150자
            - 잘한 점: 근거가 있는 범위에서 2~3개 항목, 각 항목은 최소 100자로 영역 점수나 구체적 분석 근거 포함
            - 진행 맥락: 근거가 있는 범위에서 2~3개 항목, 각 항목은 최소 100자로 마디·코드 진행·음표 중 입력에 존재하는 근거 포함
            - 개선 제안: 2~3개 항목, 각 항목은 최소 100자로 문제점·근거·실행 가능한 연습 방법 포함
            - 점수 요약: 종합 점수와 네 영역 점수를 입력값 그대로 표시
            - 전체 본문: 700자 이상 1,500자 이하

            사용자를 비난하지 말고 구체적인 마디·코드·음표 근거를 우선 제시하세요.
            점수와 수치를 임의로 만들거나 변경하지 마세요.
            근거가 부족하면 항목 수를 억지로 채우지 말고, 입력 JSON에 없는 사실을 만들지 마세요.
            """;

    private final GeminiClient geminiClient;
    private final GeminiProperties properties;
    private final RuleBasedReportGenerator ruleBasedReportGenerator;
    private final ObjectMapper objectMapper;

    public GeneratedAnalysisReport generate(JsonNode analysisResult) {
        long startedAt = System.nanoTime();
        String input = analysisResult.toString();
        JsonNode promptSnapshot = promptSnapshot(analysisResult);
        String inputHash = sha256(PROMPT_VERSION + ":" + input);
        try {
            GeminiGenerationResult result = geminiClient.generateReport(SYSTEM_PROMPT, input);
            validateMarkdownStructure(result.content());
            return new GeneratedAnalysisReport(
                    ReportGenerationType.LLM,
                    result.content(),
                    properties.model(),
                    PROMPT_VERSION,
                    new LlmCallMetadata(
                            LlmCallStatus.SUCCESS,
                            properties.model(),
                            PROMPT_VERSION,
                            promptSnapshot,
                            result.promptTokens(),
                            result.completionTokens(),
                            result.totalTokens(),
                            TEMPERATURE,
                            elapsedMillis(startedAt),
                            result.cacheHit(),
                            inputHash,
                            null
                    )
            );
        } catch (Exception exception) {
            log.warn("Gemini report generation failed; using rule-based fallback.", exception);
            return new GeneratedAnalysisReport(
                    ReportGenerationType.RULE_BASED,
                    ruleBasedReportGenerator.generate(analysisResult),
                    null,
                    PROMPT_VERSION,
                    new LlmCallMetadata(
                            isTimeout(exception) ? LlmCallStatus.TIMEOUT : LlmCallStatus.FAILED,
                            properties.model(),
                            PROMPT_VERSION,
                            promptSnapshot,
                            null,
                            null,
                            null,
                            TEMPERATURE,
                            elapsedMillis(startedAt),
                            false,
                            inputHash,
                            exception.getMessage()
                    )
            );
        }
    }

    private void validateMarkdownStructure(String content) {
        if (content == null || content.strip().length() < MIN_REPORT_LENGTH) {
            throw new IllegalStateException("Gemini returned a report that is too short.");
        }
        List<String> lines = content.lines()
                .map(String::stripTrailing)
                .toList();
        int previousIndex = -1;
        for (String heading : REQUIRED_HEADINGS) {
            int currentIndex = lines.subList(previousIndex + 1, lines.size()).indexOf(heading);
            if (currentIndex < 0) {
                throw new IllegalStateException("Gemini returned an invalid report structure.");
            }
            previousIndex += currentIndex + 1;
        }
    }

    private JsonNode promptSnapshot(JsonNode analysisResult) {
        var snapshot = objectMapper.createObjectNode();
        snapshot.put("promptVersion", PROMPT_VERSION);
        snapshot.put("systemPrompt", SYSTEM_PROMPT);
        snapshot.set("analysisResult", analysisResult);
        return snapshot;
    }

    private int elapsedMillis(long startedAt) {
        long elapsed = (System.nanoTime() - startedAt) / 1_000_000L;
        return (int) Math.min(elapsed, Integer.MAX_VALUE);
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException || current instanceof HttpTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
