package com.mr.domain.statistics.service;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.statistics.entity.enums.SkillType;
import java.math.BigDecimal;

final class AnalysisSkillScoreResolver {

    private AnalysisSkillScoreResolver() {
    }

    static BigDecimal resolve(Analysis analysis, SkillType skillType) {
        return switch (skillType) {
            case SCALE -> analysis.getScaleScore();
            case TENSION -> analysis.getTensionScore();
            case PROGRESSION -> analysis.getProgressionScore();
            case VOICE_LEADING -> analysis.getVoiceLeadingScore();
        };
    }
}
