package com.mr.domain.analysis.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import org.springframework.stereotype.Component;

@Component
public class AnalysisResultEnricher {

    public JsonNode enrich(JsonNode result) {
        String existingSummary = result.path("summary").asText();
        if (!existingSummary.isBlank()) {
            return result;
        }

        ObjectNode enriched = ((ObjectNode) result).deepCopy();
        enriched.put("summary", generateSummary(result));
        return enriched;
    }

    private String generateSummary(JsonNode result) {
        JsonNode scores = result.path("scores");
        var domainScores = new ArrayList<DomainScore>();
        scores.path("domains").fields().forEachRemaining(entry -> {
            if (entry.getValue().isNumber()) {
                domainScores.add(new DomainScore(entry.getKey(), entry.getValue().decimalValue()));
            }
        });
        DomainScore strongest = domainScores.stream()
                .max(Comparator.comparing(DomainScore::score))
                .orElseThrow(() -> new IllegalStateException("도메인 점수가 없습니다."));
        return switch (strongest.name()) {
            case "스케일" -> "조성에 어울리는 음 선택이 안정적으로 이어졌어요.";
            case "텐션" -> "긴장감을 살리는 텐션 활용이 자연스럽게 이어졌어요.";
            case "진행" -> "코드 진행의 흐름을 안정적으로 잘 따라갔어요.";
            case "코드 연결" -> "코드 사이의 음 연결이 자연스럽고 매끄러웠어요.";
            default -> strongest.name() + " 영역의 흐름이 전반적으로 안정적이었어요.";
        };
    }

    private record DomainScore(String name, BigDecimal score) {
    }
}
