package com.mr.domain.analysis.entity;

import com.mr.domain.analysis.entity.enums.AnalysisGrade;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
// TODO: 추후 User, Playing 도메인 인덱스 추가
@Table(
        name = "analysis",
        indexes = {
                @Index(name = "idx_analysis_status", columnList = "status")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Analysis extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long id;

    /** TODO: User 도메인 엔티티 연관관계 연결 예정 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** TODO: Playing 엔티티 연관관계 연결 예정 */
    @Column(name = "playing_id", nullable = false)
    private Long playingId;

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

    @Column(name = "raw_result_json", columnDefinition = "json")
    private String rawResultJson;

    @Column(name = "failed_reason", columnDefinition = "text")
    private String failedReason;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Analysis(Long userId, Long playingId, String analysisVersion, Integer startBar, Integer endBar,
                     AnalysisStatus status, Integer totalScore, AnalysisGrade grade, String summary,
                     String analysisRequestJson, BigDecimal scaleScore, BigDecimal tensionScore,
                     BigDecimal progressionScore, BigDecimal voiceLeadingScore, String rawResultJson,
                     String failedReason, LocalDateTime completedAt) {
        validatePositive(startBar, "startBar");
        validatePositive(endBar, "endBar");
        validateBarRange(startBar, endBar);

        this.userId = userId;
        this.playingId = playingId;
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

    public static Analysis createPending(Long userId, Long playingId, Integer startBar, Integer endBar,
                                         String analysisRequestJson) {
        return Analysis.builder()
                .userId(userId)
                .playingId(playingId)
                .analysisVersion("v1")
                .startBar(startBar)
                .endBar(endBar)
                .status(AnalysisStatus.PENDING)
                .analysisRequestJson(analysisRequestJson)
                .build();
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

    public void startProcessing() {
        if (this.status != AnalysisStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태의 분석만 PROCESSING으로 변경할 수 있습니다.");
        }
        this.status = AnalysisStatus.PROCESSING;
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
        if (this.status == AnalysisStatus.COMPLETED) {
            throw new IllegalStateException("이미 완료된 분석은 실패 처리할 수 없습니다.");
        }
        this.status = AnalysisStatus.FAILED;
        this.failedReason = failedReason;
        this.completedAt = LocalDateTime.now();
    }
}
