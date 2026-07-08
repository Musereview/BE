package com.mr.domain.mentor.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.AnalysisReport;
import com.mr.domain.mentor.entity.enums.LlmCallStatus;
import com.mr.domain.mentor.entity.enums.LlmPurpose;
import com.mr.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "llm_call_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LlmCallLog extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "llm_call_log_id")
    private Long id;

    /** TODO: User 도메인 엔티티 연관관계 연결 예정 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id")
    private Analysis analysis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_report_id")
    private AnalysisReport analysisReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_message_id")
    private MentorMessage mentorMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", length = 30)
    private LlmPurpose purpose;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "prompt_version", length = 30)
    private String promptVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prompt_snapshot_json", columnDefinition = "json")
    private JsonNode promptSnapshotJson;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "temperature", precision = 3, scale = 2)
    private BigDecimal temperature;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "cache_hit", nullable = false)
    private Boolean cacheHit;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private LlmCallStatus status;

    @Column(name = "input_hash", nullable = false, length = 255)
    private String inputHash;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Builder
    private LlmCallLog(Long userId, Analysis analysis, AnalysisReport analysisReport,
                       MentorMessage mentorMessage, LlmPurpose purpose, String modelName, String promptVersion,
                       JsonNode promptSnapshotJson, Integer promptTokens, Integer completionTokens,
                       Integer totalTokens, BigDecimal temperature, Integer latencyMs, Boolean cacheHit,
                       LlmCallStatus status, String inputHash, String errorMessage) {
        this.userId = userId;
        this.analysis = analysis;
        this.analysisReport = analysisReport;
        this.mentorMessage = mentorMessage;
        this.purpose = purpose;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.promptSnapshotJson = promptSnapshotJson;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
        this.temperature = temperature;
        this.latencyMs = latencyMs;
        this.cacheHit = cacheHit != null && cacheHit;
        this.status = status;
        this.inputHash = inputHash;
        this.errorMessage = errorMessage;
    }

    public static LlmCallLog success(Long userId, Analysis analysis, AnalysisReport analysisReport,
                                     MentorMessage mentorMessage, LlmPurpose purpose, String modelName, String promptVersion,
                                     JsonNode promptSnapshotJson, Integer promptTokens, Integer completionTokens,
                                     Integer totalTokens, BigDecimal temperature, Integer latencyMs, Boolean cacheHit,
                                     String inputHash) {
        return LlmCallLog.builder()
                .userId(userId)
                .analysis(analysis)
                .analysisReport(analysisReport)
                .mentorMessage(mentorMessage)
                .purpose(purpose)
                .modelName(modelName)
                .promptVersion(promptVersion)
                .promptSnapshotJson(promptSnapshotJson)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(totalTokens)
                .temperature(temperature)
                .latencyMs(latencyMs)
                .cacheHit(cacheHit)
                .status(LlmCallStatus.SUCCESS)
                .inputHash(inputHash)
                .build();
    }

    public static LlmCallLog failed(Long userId, Analysis analysis, AnalysisReport analysisReport,
                                    MentorMessage mentorMessage, LlmPurpose purpose, String modelName, String promptVersion,
                                    JsonNode promptSnapshotJson, BigDecimal temperature, Integer latencyMs, String inputHash,
                                    String errorMessage) {
        return LlmCallLog.builder()
                .userId(userId)
                .analysis(analysis)
                .analysisReport(analysisReport)
                .mentorMessage(mentorMessage)
                .purpose(purpose)
                .modelName(modelName)
                .promptVersion(promptVersion)
                .promptSnapshotJson(promptSnapshotJson)
                .temperature(temperature)
                .latencyMs(latencyMs)
                .cacheHit(false)
                .status(LlmCallStatus.FAILED)
                .inputHash(inputHash)
                .errorMessage(errorMessage)
                .build();
    }

    public static LlmCallLog timeout(Long userId, Analysis analysis, AnalysisReport analysisReport,
                                     MentorMessage mentorMessage, LlmPurpose purpose, String modelName, String promptVersion,
                                     JsonNode promptSnapshotJson, BigDecimal temperature, Integer latencyMs, String inputHash,
                                     String errorMessage) {
        return LlmCallLog.builder()
                .userId(userId)
                .analysis(analysis)
                .analysisReport(analysisReport)
                .mentorMessage(mentorMessage)
                .purpose(purpose)
                .modelName(modelName)
                .promptVersion(promptVersion)
                .promptSnapshotJson(promptSnapshotJson)
                .temperature(temperature)
                .latencyMs(latencyMs)
                .cacheHit(false)
                .status(LlmCallStatus.TIMEOUT)
                .inputHash(inputHash)
                .errorMessage(errorMessage)
                .build();
    }
}
