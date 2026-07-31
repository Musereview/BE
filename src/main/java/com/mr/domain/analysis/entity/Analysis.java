package com.mr.domain.analysis.entity;

import com.mr.domain.analysis.entity.enums.AnalysisGrade;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.exception.AnalysisErrorStatus;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.user.entity.User;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
        name = "analysis",
        indexes = {
                @Index(name = "idx_analysis_status", columnList = "status"),
                @Index(name = "idx_analysis_user_id", columnList = "user_id"),
                @Index(name = "idx_analysis_playing_id", columnList = "playing_id")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Analysis extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playing_id", nullable = false)
    private Playing playing;

    @Column(name = "analysis_version", nullable = false, length = 50)
    private String analysisVersion;

    @Column(name = "start_bar", nullable = false)
    private Integer startBar;

    @Column(name = "end_bar", nullable = false)
    private Integer endBar;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AnalysisStatus status;

    @Column(name = "total_score")
    private Integer totalScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade", length = 20)
    private AnalysisGrade grade;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis_request_json", nullable = false, columnDefinition = "json")
    private String analysisRequestJson;

    @Column(name = "scale_score", precision = 5, scale = 2)
    private BigDecimal scaleScore;

    @Column(name = "tension_score", precision = 5, scale = 2)
    private BigDecimal tensionScore;

    @Column(name = "progression_score", precision = 5, scale = 2)
    private BigDecimal progressionScore;

    @Column(name = "voice_leading_score", precision = 5, scale = 2)
    private BigDecimal voiceLeadingScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_result_json", columnDefinition = "json")
    private String rawResultJson;

    @Column(name = "failed_reason", columnDefinition = "text")
    private String failedReason;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Analysis(User user, Playing playing, String analysisVersion, Integer startBar, Integer endBar,
                     AnalysisStatus status, Integer totalScore, AnalysisGrade grade, String summary,
                     String analysisRequestJson, BigDecimal scaleScore, BigDecimal tensionScore,
                     BigDecimal progressionScore, BigDecimal voiceLeadingScore, String rawResultJson,
                     String failedReason, LocalDateTime completedAt) {
        validateUser(user);
        validatePlaying(playing);
        validateOwnerConsistency(user, playing);
        validatePositive(startBar, "startBar");
        validatePositive(endBar, "endBar");
        validateBarRange(startBar, endBar);

        this.user = user;
        this.playing = playing;
        this.analysisVersion = analysisVersion;
        this.startBar = startBar;
        this.endBar = endBar;
        this.status = status;
        this.totalScore = totalScore;
        this.grade = grade;
        this.summary = summary;
        this.analysisRequestJson = analysisRequestJson;
        this.scaleScore = scaleScore;
        this.tensionScore = tensionScore;
        this.progressionScore = progressionScore;
        this.voiceLeadingScore = voiceLeadingScore;
        this.rawResultJson = rawResultJson;
        this.failedReason = failedReason;
        this.completedAt = completedAt;
    }

    public static Analysis createPending(User user, Playing playing, Integer startBar, Integer endBar,
                                         String analysisRequestJson) {
        return Analysis.builder()
                .user(user)
                .playing(playing)
                .analysisVersion("v1")
                .startBar(startBar)
                .endBar(endBar)
                .status(AnalysisStatus.PENDING)
                .analysisRequestJson(analysisRequestJson)
                .build();
    }

    private static void validateUser(User user) {
        if (user == null) {
            throw new GeneralException(AnalysisErrorStatus.ANALYSIS_INVALID_REQUEST);
        }
    }

    private static void validatePlaying(Playing playing) {
        if (playing == null) {
            throw new GeneralException(AnalysisErrorStatus.ANALYSIS_INVALID_REQUEST);
        }
    }

    private static void validateOwnerConsistency(User user, Playing playing) {
        if (!Objects.equals(user.getUserId(), playing.getUser().getUserId())) {
            throw new GeneralException(AnalysisErrorStatus.ANALYSIS_OWNER_MISMATCH);
        }
    }

    private static void validatePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수이며 양수여야 합니다.");
        }
    }

    private static void validateBarRange(Integer startBar, Integer endBar) {
        if (startBar > endBar) {
            throw new IllegalArgumentException("startBar는 endBar보다 클 수 없습니다.");
        }
    }

    public LocalDateTime startProcessing() {
        if (this.status != AnalysisStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태의 분석만 PROCESSING으로 변경할 수 있습니다.");
        }
        this.status = AnalysisStatus.PROCESSING;
        this.processingStartedAt = nextProcessingStartedAt();
        return this.processingStartedAt;
    }

    public LocalDateTime restartProcessing() {
        if (this.status != AnalysisStatus.PROCESSING) {
            throw new IllegalStateException("PROCESSING 상태의 분석만 다시 시작할 수 있습니다.");
        }
        this.processingStartedAt = nextProcessingStartedAt();
        return this.processingStartedAt;
    }

    public boolean isCurrentProcessing(LocalDateTime expectedProcessingStartedAt) {
        return this.status == AnalysisStatus.PROCESSING
                && Objects.equals(this.processingStartedAt, expectedProcessingStartedAt);
    }

    private LocalDateTime nextProcessingStartedAt() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        if (this.processingStartedAt != null && !now.isAfter(this.processingStartedAt)) {
            return this.processingStartedAt.plus(1, ChronoUnit.MILLIS);
        }
        return now;
    }

    public void complete(Integer totalScore, AnalysisGrade grade, String summary, BigDecimal scaleScore,
                         BigDecimal tensionScore, BigDecimal progressionScore, BigDecimal voiceLeadingScore,
                         String rawResultJson) {
        if (this.status != AnalysisStatus.PROCESSING) {
            throw new IllegalStateException("PROCESSING 상태의 분석만 완료 처리할 수 있습니다.");
        }
        this.status = AnalysisStatus.COMPLETED;
        this.totalScore = totalScore;
        this.grade = grade;
        this.summary = summary;
        this.scaleScore = scaleScore;
        this.tensionScore = tensionScore;
        this.progressionScore = progressionScore;
        this.voiceLeadingScore = voiceLeadingScore;
        this.rawResultJson = rawResultJson;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String failedReason) {
        if (this.status == AnalysisStatus.COMPLETED || this.status == AnalysisStatus.FAILED) {
            throw new IllegalStateException("이미 완료된 분석은 실패 처리할 수 없습니다.");
        }
        this.status = AnalysisStatus.FAILED;
        this.failedReason = failedReason;
        this.completedAt = LocalDateTime.now();
    }
}
