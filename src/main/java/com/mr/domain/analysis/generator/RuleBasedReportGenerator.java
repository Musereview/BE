package com.mr.domain.analysis.generator;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.Set;
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
                .append(strongestAndWeakest(domains)).append(" ")
                .append(text(result, "summary", "영역별 점수를 기준으로 강점과 보완점을 정리했습니다."))
                .append("\n\n");

        report.append("## 잘한 점\n\n")
                .append("- ").append(strongestDomain(domains))
                .append("으로 네 영역 중 가장 높은 평가를 받았습니다.\n");
        appendCoverage(report, scores.path("coverage"));
        report.append("\n");

        report.append("## 진행 맥락\n\n");
        JsonNode rules = result.path("harmonic_rules");
        if (rules.isArray() && !rules.isEmpty()) {
            Set<String> labels = new LinkedHashSet<>();
            for (JsonNode rule : rules) {
                labels.add(text(rule, "label", text(rule, "rule", "감지된 화성 진행")));
            }
            labels.stream().limit(5).forEach(label -> report.append("- ").append(label).append("\n"));
        } else {
            report.append("- 감지된 화성 진행을 바탕으로 연주를 분석했습니다.\n");
        }

        report.append("\n## 개선 제안\n\n")
                .append("- ").append(weakestDomain(domains))
                .append(" 영역이 상대적으로 낮습니다. 백킹트랙 속도를 낮추고 같은 구간을 반복해 보세요.\n");
        appendScaleSuggestion(report, scores.path("scale_appropriateness"));
        appendTimingSuggestion(report, result.path("timing_deviations"));
        appendLearningRecommendations(report, result.path("learning_recommendations"));

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

    private void appendCoverage(StringBuilder report, JsonNode coverage) {
        String note = coverage.path("note").asText();
        if (!note.isBlank()) {
            report.append("- 분석 범위: ").append(note).append("\n");
        }
    }

    private void appendScaleSuggestion(StringBuilder report, JsonNode scaleAppropriateness) {
        JsonNode notes = scaleAppropriateness.path("out_of_scale_notes");
        if (!notes.isArray() || notes.isEmpty()) {
            return;
        }
        JsonNode first = notes.path(0);
        report.append("- 스케일 밖 음이 ").append(notes.size()).append("개 감지되었습니다.");
        String suggestion = first.path("suggestion").asText();
        if (!suggestion.isBlank()) {
            report.append(" 예: ").append(suggestion);
        }
        report.append("\n");
    }

    private void appendLearningRecommendations(StringBuilder report, JsonNode recommendations) {
        if (!recommendations.isArray()) {
            return;
        }
        int count = Math.min(recommendations.size(), 2);
        for (int index = 0; index < count; index++) {
            JsonNode recommendation = recommendations.path(index);
            String title = text(recommendation, "title", "추천 학습");
            String reason = recommendation.path("reason").asText();
            String studyTip = recommendation.path("study_tip").asText();
            report.append("- ").append(title).append(": ");
            if (!reason.isBlank()) {
                report.append(reason).append(" ");
            }
            if (!studyTip.isBlank()) {
                report.append(studyTip);
            }
            report.append("\n");
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
