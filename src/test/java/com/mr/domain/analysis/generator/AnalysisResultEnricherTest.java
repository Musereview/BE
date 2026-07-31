package com.mr.domain.analysis.generator;

import static org.assertj.core.api.Assertions.assertThat;

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
                .isEqualTo("조성에 어울리는 음 선택이 안정적으로 이어졌어요.")
                .doesNotContain("80.5", "점", "좋음")
                .hasSizeBetween(20, 40);
        assertThat(original.has("summary")).isFalse();
    }

    @Test
    void enrich_describesTensionWithoutRepeatingScore() throws Exception {
        JsonNode original = resultWithoutSummary();
        ((com.fasterxml.jackson.databind.node.ObjectNode) original.path("scores").path("domains"))
                .put("텐션", 100);

        JsonNode enriched = enricher.enrich(original);

        assertThat(enriched.path("summary").asText())
                .isEqualTo("긴장감을 살리는 텐션 활용이 자연스럽게 이어졌어요.")
                .doesNotContain("100", "점");
    }

    @Test
    void enrich_preservesExistingSummary() throws Exception {
        JsonNode original = resultWithoutSummary();
        ((com.fasterxml.jackson.databind.node.ObjectNode) original).put("summary", "AI 서버 요약");

        JsonNode enriched = enricher.enrich(original);

        assertThat(enriched).isSameAs(original);
        assertThat(enriched.path("summary").asText()).isEqualTo("AI 서버 요약");
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
