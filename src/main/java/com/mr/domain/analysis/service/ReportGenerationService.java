package com.mr.domain.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.analysis.entity.enums.ReportGenerationType;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    static final String PROMPT_VERSION = "analysis-report-v1";
    private static final BigDecimal TEMPERATURE = new BigDecimal("0.30");
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

            사용자를 비난하지 말고 구체적인 마디·코드·음표 근거를 우선 제시하세요.
            점수와 수치를 임의로 만들거나 변경하지 마세요.
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
