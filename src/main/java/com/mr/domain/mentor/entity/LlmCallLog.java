package com.mr.domain.mentor.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.AnalysisReport;
import com.mr.domain.mentor.entity.enums.LlmCallStatus;
import com.mr.domain.mentor.entity.enums.LlmPurpose;
import com.mr.domain.mentor.exception.MentorErrorStatus;
import com.mr.domain.user.entity.User;
import com.mr.global.apipayload.exception.GeneralException;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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
    private LlmCallLog(User user, Analysis analysis, AnalysisReport analysisReport,
                       MentorMessage mentorMessage, LlmPurpose purpose, String modelName, String promptVersion,
                       JsonNode promptSnapshotJson, Integer promptTokens, Integer completionTokens,
                       Integer totalTokens, BigDecimal temperature, Integer latencyMs, Boolean cacheHit,
                       LlmCallStatus status, String inputHash, String errorMessage) {
        validateUser(user);

        this.user = user;
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

    private static void validateUser(User user) {
        if (user == null) {
            throw new GeneralException(MentorErrorStatus.MENTOR_INVALID_REQUEST);
        }
    }

    private static LlmCallLogBuilder baseBuilder(User user, Analysis analysis, AnalysisReport analysisReport,
                                                 MentorMessage mentorMessage, LlmPurpose purpose, String modelName, String promptVersion,
                                                 JsonNode promptSnapshotJson, BigDecimal temperature, Integer latencyMs, String inputHash) {
        return LlmCallLog.builder()
                .user(user)
                .analysis(analysis)
                .analysisReport(analysisReport)
                .mentorMessage(mentorMessage)
                .purpose(purpose)
                .modelName(modelName)
                .promptVersion(promptVersion)
                .promptSnapshotJson(promptSnapshotJson)
                .temperature(temperature)
                .latencyMs(latencyMs)
                .inputHash(inputHash);
    }

    public static LlmCallLog success(User user, Analysis analysis, AnalysisReport analysisReport,
                                     MentorMessage mentorMessage, LlmPurpose purpose, String modelName, String promptVersion,
                                     JsonNode promptSnapshotJson, Integer promptTokens, Integer completionTokens,
                                     Integer totalTokens, BigDecimal temperature, Integer latencyMs, Boolean cacheHit,
                                     String inputHash) {
        return baseBuilder(
                user,
                analysis,
                analysisReport,
                mentorMessage,
                purpose,
                modelName,
                promptVersion,
                promptSnapshotJson,
                temperature,
                latencyMs,
                inputHash
        )
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(totalTokens)
                .cacheHit(cacheHit)
                .status(LlmCallStatus.SUCCESS)
                .build();
    }

    public static LlmCallLog failed(User user, Analysis analysis, AnalysisReport analysisReport,
                                    MentorMessage mentorMessage, LlmPurpose purpose, String modelName, String promptVersion,
                                    JsonNode promptSnapshotJson, BigDecimal temperature, Integer latencyMs, String inputHash,
                                    String errorMessage) {
        return baseBuilder(
                user,
                analysis,
                analysisReport,
                mentorMessage,
                purpose,
                modelName,
                promptVersion,
                promptSnapshotJson,
                temperature,
                latencyMs,
                inputHash
        )
                .cacheHit(false)
                .status(LlmCallStatus.FAILED)
                .errorMessage(errorMessage)
                .build();
    }

    public static LlmCallLog timeout(User user, Analysis analysis, AnalysisReport analysisReport,
                                     MentorMessage mentorMessage, LlmPurpose purpose, String modelName, String promptVersion,
                                     JsonNode promptSnapshotJson, BigDecimal temperature, Integer latencyMs, String inputHash,
                                     String errorMessage) {
        return baseBuilder(
                user,
                analysis,
                analysisReport,
                mentorMessage,
                purpose,
                modelName,
                promptVersion,
                promptSnapshotJson,
                temperature,
                latencyMs,
                inputHash
        )
                .cacheHit(false)
                .status(LlmCallStatus.TIMEOUT)
                .errorMessage(errorMessage)
                .build();
    }
}
