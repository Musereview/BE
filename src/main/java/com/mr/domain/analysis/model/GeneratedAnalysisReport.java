package com.mr.domain.analysis.model;

import com.mr.domain.analysis.entity.enums.ReportGenerationType;

public record GeneratedAnalysisReport(
        ReportGenerationType generationType,
        String content,
        String modelName,
        String promptVersion,
        LlmCallMetadata llmCall
) {
}
