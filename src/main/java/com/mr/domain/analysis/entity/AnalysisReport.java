package com.mr.domain.analysis.entity;

import com.mr.domain.analysis.entity.enums.ContentFormat;
import com.mr.domain.analysis.entity.enums.ReportGenerationType;
import com.mr.domain.analysis.entity.enums.LlmStatus;
import com.mr.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "analysis_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_type", nullable = false, length = 50)
    private ReportGenerationType generationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "llm_status", nullable = false, length = 20)
    private LlmStatus llmStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_format", nullable = false, length = 20)
    private ContentFormat contentFormat;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "prompt_version", length = 100)
    private String promptVersion;

    private AnalysisReport(Analysis analysis, ReportGenerationType generationType, LlmStatus llmStatus,
                           ContentFormat contentFormat, String content, String modelName, String promptVersion) {
        this.analysis = analysis;
        this.generationType = generationType;
        this.llmStatus = llmStatus;
        this.contentFormat = contentFormat;
        this.content = content;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
    }

    public static AnalysisReport createLlmReport(Analysis analysis, String content, String modelName,
                                                 String promptVersion) {
        return new AnalysisReport(
                analysis,
                ReportGenerationType.LLM,
                LlmStatus.SUCCESS,
                ContentFormat.MARKDOWN,
                content,
                modelName,
                promptVersion
        );
    }

    public static AnalysisReport createRuleBasedReport(Analysis analysis, String content) {
        return new AnalysisReport(
                analysis,
                ReportGenerationType.RULE_BASED,
                LlmStatus.SUCCESS,
                ContentFormat.MARKDOWN,
                content,
                null,
                null
        );
    }

    public void updateContent(LlmStatus llmStatus, String content) {
        this.llmStatus = llmStatus;
        this.content = content;
    }
}
