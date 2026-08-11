package com.mr.domain.analysis.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AnalysisResultEnricherTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AnalysisResultEnricher enricher = new AnalysisResultEnricher();

    @Test
    void enrich_addsEvidenceBasedSummaryWhenMissing() throws Exception {
        JsonNode original = resultWithoutSummary();

        JsonNode enriched = enricher.enrich(original);

        assertThat(enriched.path("summary").asText())
                .contains("조성에 어울리는 음 선택이 가장 안정적이었고")
                .contains("텐션 선택의 정확도를 더 다듬을 필요가 있으며")
                .contains("강점과 보완 영역의 차이가 뚜렷하고")
                .doesNotContain("80.5", "95점", "좋음")
                .hasSizeBetween(50, 150)
                .matches("^[^.!?。！？]*[.!?。！？]$");
        assertThat(original.has("summary")).isFalse();
    }

    @Test
    void enrich_describesTensionWithoutRepeatingScore() throws Exception {
        JsonNode original = resultWithoutSummary();
        ((com.fasterxml.jackson.databind.node.ObjectNode) original.path("scores").path("domains"))
                .put("텐션", 100);

        JsonNode enriched = enricher.enrich(original);

        assertThat(enriched.path("summary").asText())
                .contains("긴장감을 살리는 텐션 활용이 가장 돋보였고")
                .doesNotContain("100", "100점");
    }

    @Test
    void enrich_generatesDifferentSummariesFromDifferentAnalysisEvidence() throws Exception {
        JsonNode timingResult = resultWithoutSummary();
        ((com.fasterxml.jackson.databind.node.ObjectNode) timingResult).set(
                "timing_deviations",
                objectMapper.readTree("{\"summary\":{\"flagged_count\":4}}")
        );
        JsonNode harmonicResult = resultWithoutSummary();
        ((com.fasterxml.jackson.databind.node.ObjectNode) harmonicResult).set(
                "harmonic_rules",
                objectMapper.readTree("[{\"label\":\"ii-V-I\"}]")
        );

        String timingSummary = enricher.enrich(timingResult).path("summary").asText();
        String harmonicSummary = enricher.enrich(harmonicResult).path("summary").asText();

        assertThat(timingSummary).contains("박자가 흔들린 음이 4개");
        assertThat(harmonicSummary).contains("ii-V-I 진행의 특징");
        assertThat(timingSummary).isNotEqualTo(harmonicSummary);
    }

    @Test
    void enrich_describesBalancedScoresWhenDomainGapIsSmall() throws Exception {
        JsonNode original = resultWithoutSummary();
        var domains = (com.fasterxml.jackson.databind.node.ObjectNode) original.path("scores").path("domains");
        domains.put("스케일", 84).put("텐션", 81).put("진행", 83).put("코드 연결", 82);

        JsonNode enriched = enricher.enrich(original);

        assertThat(enriched.path("summary").asText()).contains("영역별 점수도 고르게 나타났으며");
    }

    @Test
    void enrich_preservesExistingSummary() throws Exception {
        JsonNode original = resultWithoutSummary();
        ((com.fasterxml.jackson.databind.node.ObjectNode) original).put("summary", "AI 서버 요약");

        JsonNode enriched = enricher.enrich(original);

        assertThat(enriched).isSameAs(original);
        assertThat(enriched.path("summary").asText()).isEqualTo("AI 서버 요약");
    }

    @Test
    void regenerateSummary_replacesExistingSummaryWithoutChangingOriginal() throws Exception {
        JsonNode original = resultWithoutSummary();
        ((com.fasterxml.jackson.databind.node.ObjectNode) original).put("summary", "기존 요약");

        JsonNode regenerated = enricher.regenerateSummary(original);

        assertThat(regenerated.path("summary").asText())
                .isNotEqualTo("기존 요약")
                .contains("조성에 어울리는 음 선택");
        assertThat(original.path("summary").asText()).isEqualTo("기존 요약");
    }

    @Test
    void withSummary_replacesSummaryWithoutChangingOriginal() throws Exception {
        JsonNode original = resultWithoutSummary();
        ((com.fasterxml.jackson.databind.node.ObjectNode) original).put("summary", "기존 요약");

        JsonNode enriched = enricher.withSummary(original, "LLM이 생성한 새로운 요약");

        assertThat(enriched.path("summary").asText()).isEqualTo("LLM이 생성한 새로운 요약");
        assertThat(original.path("summary").asText()).isEqualTo("기존 요약");
    }

    @Test
    void enrich_withoutDomainScores_throwsDescriptiveException() throws Exception {
        JsonNode result = objectMapper.readTree("""
                {"scores":{"domains":{}}}
                """);

        assertThatThrownBy(() -> enricher.enrich(result))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("도메인 점수가 없습니다.");
    }

    private JsonNode resultWithoutSummary() throws Exception {
        return objectMapper.readTree("""
                {
                  "scores": {
                    "final_score": 80.5,
                    "grade": "좋음",
                    "domains": {
                      "스케일": 95,
                      "텐션": 60,
                      "진행": 85,
                      "코드 연결": 75
                    }
                  }
                }
                """);
    }
}
