package com.mr.domain.analysis.generator;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class RuleBasedReportGeneratorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RuleBasedReportGenerator generator = new RuleBasedReportGenerator();

    @Test
    void generate_usesAvailableAnalysisEvidenceAndRemovesDuplicateRules() throws Exception {
        JsonNode result = objectMapper.readTree("""
                {
                  "meta": {"key":"C major","genre":"jazz","time_signature":[4,4],"bpm":120},
                  "summary": "코드 진행은 안정적이지만 코드 연결을 보완하면 좋습니다.",
                  "scores": {
                    "final_score": 80,
                    "domains": {"스케일":90,"텐션":70,"진행":85,"코드 연결":60},
                    "coverage": {"note":"충분한 길이로 분석되었습니다."},
                    "scale_appropriateness": {
                      "out_of_scale_notes": [
                        {"suggestion":"B음을 Bb로 해결하세요."},
                        {"suggestion":"F#음을 G로 해결하세요."}
                      ]
                    }
                  },
                  "harmonic_rules": [{"label":"ii-V-I"},{"label":"ii-V-I"}],
                  "timing_deviations": {"summary": {"flagged_count": 4}},
                  "learning_recommendations": [
                    {"title":"보이스 리딩","reason":"도약이 큽니다.","study_tip":"가까운 코드톤을 연결하세요."}
                  ]
                }
                """);

        String report = generator.generate(result);

        assertThat(report)
                .contains("코드 진행은 안정적", "충분한 길이", "스케일 밖 음이 2개")
                .contains("박자가 흔들린 음이 4개", "보이스 리딩", "가까운 코드톤")
                .containsOnlyOnce("- ii-V-I");
    }
}
