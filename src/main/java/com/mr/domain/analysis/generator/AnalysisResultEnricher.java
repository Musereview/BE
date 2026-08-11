package com.mr.domain.analysis.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AnalysisResultEnricher {

    public JsonNode enrich(JsonNode result) {
        String existingSummary = result.path("summary").asText();
        if (!existingSummary.isBlank()) {
            return result;
        }

        return regenerateSummary(result);
    }

    public JsonNode regenerateSummary(JsonNode result) {
        ObjectNode enriched = ((ObjectNode) result).deepCopy();
        enriched.put("summary", generateSummary(result));
        return enriched;
    }

    public JsonNode withSummary(JsonNode result, String summary) {
        ObjectNode enriched = ((ObjectNode) result).deepCopy();
        enriched.put("summary", summary);
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
        if (domainScores.isEmpty()) {
            throw new IllegalStateException("도메인 점수가 없습니다.");
        }
        List<DomainScore> rankedScores = domainScores.stream()
                .sorted(Comparator.comparing(DomainScore::score).reversed()
                        .thenComparing(DomainScore::name))
                .toList();
        DomainScore strongest = rankedScores.get(0);
        DomainScore weakest = rankedScores.get(rankedScores.size() - 1);

        StringBuilder summary = new StringBuilder(strengthClause(strongest.name()));
        BigDecimal scoreGap = strongest.score().subtract(weakest.score());
        if (rankedScores.size() > 1 && scoreGap.compareTo(new BigDecimal("5")) <= 0) {
            summary.append(" 영역별 점수도 고르게 나타났으며");
        } else if (rankedScores.size() > 1) {
            summary.append(" 반면 ").append(improvementClause(weakest.name()));
            if (scoreGap.compareTo(new BigDecimal("20")) >= 0) {
                summary.append(" 강점과 보완 영역의 차이가 뚜렷하고");
            }
        }
        appendAnalysisConclusion(summary, result);
        return summary.toString();
    }

    private String strengthClause(String domain) {
        return switch (domain) {
            case "스케일" -> "조성에 어울리는 음 선택이 가장 안정적이었고";
            case "텐션" -> "긴장감을 살리는 텐션 활용이 가장 돋보였고";
            case "진행" -> "코드 진행의 흐름을 따라가는 힘이 가장 돋보였고";
            case "코드 연결" -> "코드 사이의 음 연결이 가장 자연스러웠고";
            default -> domain + " 영역이 가장 안정적이었고";
        };
    }

    private String improvementClause(String domain) {
        return switch (domain) {
            case "스케일" -> "조성에 맞는 음 선택을 더 다듬을 필요가 있으며";
            case "텐션" -> "텐션 선택의 정확도를 더 다듬을 필요가 있으며";
            case "진행" -> "코드 진행의 흐름을 읽는 연습이 필요하며";
            case "코드 연결" -> "코드 사이의 음 간격을 더 매끄럽게 연결할 필요가 있으며";
            default -> domain + " 영역을 우선 보완할 필요가 있으며";
        };
    }

    private void appendAnalysisConclusion(StringBuilder summary, JsonNode result) {
        int timingDeviationCount = result.path("timing_deviations")
                .path("summary").path("flagged_count").asInt(0);
        if (timingDeviationCount > 0) {
            summary.append(" 박자가 흔들린 음이 ").append(timingDeviationCount)
                    .append("개 감지되어 해당 구간의 리듬 점검이 필요한 연주였어요.");
            return;
        }

        JsonNode outOfScaleNotes = result.path("scores")
                .path("scale_appropriateness").path("out_of_scale_notes");
        if (outOfScaleNotes.isArray() && !outOfScaleNotes.isEmpty()) {
            summary.append(" 스케일 밖 음이 ").append(outOfScaleNotes.size())
                    .append("개 감지되어 음 선택을 확인할 필요가 있는 연주였어요.");
            return;
        }

        JsonNode harmonicRules = result.path("harmonic_rules");
        if (harmonicRules.isArray() && !harmonicRules.isEmpty()) {
            String rule = harmonicRules.path(0).path("label").asText();
            if (!rule.isBlank()) {
                summary.append(" 특히 ").append(removeSentenceEndings(rule))
                        .append(" 진행의 특징이 드러난 연주였어요.");
                return;
            }
        }
        summary.append(" 전반적인 강점과 보완 방향이 분명한 연주였어요.");
    }

    private String removeSentenceEndings(String value) {
        return value.replaceAll("[.!?。！？]+", "").strip();
    }

    private record DomainScore(String name, BigDecimal score) {
    }
}
