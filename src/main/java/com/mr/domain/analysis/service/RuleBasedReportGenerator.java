package com.mr.domain.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedReportGenerator {

    public String generate(JsonNode result) {
        JsonNode meta = result.path("meta");
        JsonNode scores = result.path("scores");
        JsonNode domains = scores.path("domains");

        StringBuilder report = new StringBuilder();
        report.append("# 연주 분석 리포트\n\n");
        report.append("**조성** ").append(text(meta, "key", "-"))
                .append(" · **장르** ").append(text(meta, "genre", "-"))
                .append(" · **박자** ").append(timeSignature(meta.path("time_signature")))
                .append(" · **템포** ").append(number(meta.path("bpm"))).append(" bpm\n\n");

        report.append("## 총평\n\n")
                .append("전체 점수는 **").append(number(scores.path("final_score")))
                .append(" / 100**입니다. ")
                .append(strongestAndWeakest(domains)).append("\n\n");

        report.append("## 잘한 점\n\n")
                .append("- ").append(strongestDomain(domains)).append("\n\n");

        report.append("## 진행 맥락\n\n");
        JsonNode rules = result.path("harmonic_rules");
        if (rules.isArray() && !rules.isEmpty()) {
            for (JsonNode rule : rules) {
                report.append("- ").append(text(rule, "label", text(rule, "rule", "감지된 화성 진행")))
                        .append("\n");
            }
        } else {
            report.append("- 감지된 화성 진행을 바탕으로 연주를 분석했습니다.\n");
        }

        report.append("\n## 개선 제안\n\n")
                .append("- ").append(weakestDomain(domains))
                .append(" 영역을 중심으로 느린 템포에서 반복 연습해 보세요.\n");
        appendTimingSuggestion(report, result.path("timing_deviations"));

        report.append("\n## 점수 요약\n\n")
                .append("- 종합 점수: ").append(number(scores.path("final_score"))).append(" / 100\n");
        domains.fields().forEachRemaining(entry ->
                report.append("- ").append(entry.getKey()).append(": ")
                        .append(number(entry.getValue())).append("\n"));
        return report.toString();
    }

    private String strongestAndWeakest(JsonNode domains) {
        return strongestDomain(domains) + "이 강점이며, " + weakestDomain(domains) + "을 우선 보완하면 좋습니다.";
    }

    private String strongestDomain(JsonNode domains) {
        return domainAtExtreme(domains, true);
    }

    private String weakestDomain(JsonNode domains) {
        return domainAtExtreme(domains, false);
    }

    private String domainAtExtreme(JsonNode domains, boolean maximum) {
        String selected = "연주";
        BigDecimal selectedScore = null;
        var fields = domains.fields();
        while (fields.hasNext()) {
            var field = fields.next();
            if (!field.getValue().isNumber()) {
                continue;
            }
            BigDecimal score = field.getValue().decimalValue();
            if (selectedScore == null
                    || (maximum && score.compareTo(selectedScore) > 0)
                    || (!maximum && score.compareTo(selectedScore) < 0)) {
                selected = field.getKey();
                selectedScore = score;
            }
        }
        return selected + (selectedScore == null ? "" : " (" + format(selectedScore) + "점)");
    }

    private void appendTimingSuggestion(StringBuilder report, JsonNode timing) {
        int flaggedCount = timing.path("summary").path("flagged_count").asInt(0);
        if (flaggedCount > 0) {
            report.append("- 박자가 흔들린 음이 ").append(flaggedCount)
                    .append("개 감지되었습니다. 메트로놈과 함께 해당 구간을 점검해 보세요.\n");
        }
    }

    private String timeSignature(JsonNode node) {
        return node.isArray() && node.size() == 2
                ? node.path(0).asText() + "/" + node.path(1).asText()
                : "-";
    }

    private String text(JsonNode parent, String field, String fallback) {
        String value = parent.path(field).asText();
        return value.isBlank() ? fallback : value;
    }

    private String number(JsonNode node) {
        return node.isNumber() ? format(node.decimalValue()) : "-";
    }

    private String format(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
