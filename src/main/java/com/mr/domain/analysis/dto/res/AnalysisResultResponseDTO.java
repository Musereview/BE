package com.mr.domain.analysis.dto.res;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.AnalysisReport;
import com.mr.domain.analysis.entity.enums.AnalysisGrade;
import com.mr.domain.analysis.entity.enums.ContentFormat;
import com.mr.domain.analysis.entity.enums.LlmStatus;
import com.mr.domain.analysis.entity.enums.ReportGenerationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

@Schema(description = "분석 결과 조회 응답")
public record AnalysisResultResponseDTO(
        @Schema(description = "분석 ID", example = "342")
        Long analysisId,

        @Schema(description = "분석 대상 연주 기록 ID", example = "128")
        Long playingId,

        @Schema(description = "연주에 사용한 백킹트랙 제목", example = "Autumn Leaves")
        String title,

        @Schema(description = "백킹트랙 장르", example = "JAZZ")
        String genre,

        @Schema(description = "백킹트랙 조성 (조표 + 스케일). 조표나 스케일 정보가 없으면 null", example = "Bb Major")
        String key,

        @Schema(description = "연주 BPM", example = "120")
        Integer bpm,

        @Schema(description = "연주 종료 시각", example = "2026-08-10T21:14:32")
        LocalDateTime playedAt,

        @Schema(description = "녹음 파일 다운로드용 presigned URL")
        String recordingFileUrl,

        @Schema(description = "백킹트랙 음원 다운로드용 presigned URL. 음원이 없으면 null")
        String backingTrackAudioFileUrl,

        @Schema(description = "분석 진행 상태 (조회 성공 시 항상 COMPLETED)", example = "COMPLETED")
        AnalysisStatus status,

        @Schema(description = "분석 구간 시작 마디", example = "1")
        Integer startBar,

        @Schema(description = "분석 구간 종료 마디", example = "8")
        Integer endBar,

        @Schema(description = "분석 종합 점수 (0~100)", example = "82")
        Integer totalScore,

        @Schema(description = "종합 점수에 따른 등급", example = "GOOD")
        AnalysisGrade grade,

        @Schema(description = "분석 한 줄 요약", example = "코드 전환은 안정적이나 8마디 이후 박자가 밀립니다.")
        String summary,

        @Schema(description = "영역별 점수")
        DomainScores domainScores,

        @Schema(description = "생성된 분석 리포트. 성공한 리포트가 없으면 null")
        Report report,

        @Schema(description = "AI 서버가 돌려준 원본 분석 결과(JSON). 저장된 결과가 없으면 null")
        @JsonProperty("result") JsonNode rawResult,

        @Schema(description = "분석 요청 시각", example = "2026-08-10T21:20:05")
        LocalDateTime createdAt,

        @Schema(description = "분석 완료 시각", example = "2026-08-10T21:21:40")
        LocalDateTime completedAt
) {

    public static AnalysisResultResponseDTO from(
            Analysis analysis,
            AnalysisReport analysisReport,
            JsonNode rawResult,
            String recordingFileUrl,
            String backingTrackAudioFileUrl
    ) {
        Playing playing = analysis.getPlaying();
        BackingTrack backingTrack = playing.getBackingTrack();
        return new AnalysisResultResponseDTO(
                analysis.getId(),
                playing.getId(),
                backingTrack != null ? backingTrack.getTitle() : null,
                backingTrack != null ? backingTrack.getGenre() : null,
                formatKey(backingTrack),
                playing.getBpm(),
                playing.getEndedAt(),
                recordingFileUrl,
                backingTrackAudioFileUrl,
                analysis.getStatus(),
                analysis.getStartBar(),
                analysis.getEndBar(),
                analysis.getTotalScore(),
                analysis.getGrade(),
                analysis.getSummary(),
                DomainScores.from(analysis),
                Report.fromNullable(analysisReport),
                rawResult,
                analysis.getCreatedAt(),
                analysis.getCompletedAt()
        );
    }

    private static String formatKey(BackingTrack backingTrack) {
        if (backingTrack == null || backingTrack.getKeySignature() == null || backingTrack.getScaleType() == null) {
            return null;
        }
        String scale = backingTrack.getScaleType().name().toLowerCase(Locale.ROOT);
        return backingTrack.getKeySignature()
                + " "
                + Character.toUpperCase(scale.charAt(0))
                + scale.substring(1);
    }

    @Schema(description = "영역별 분석 점수. 해당 영역이 채점되지 않았으면 null")
    public record DomainScores(
            @Schema(description = "스케일 점수 (0~100)", example = "85.5")
            @JsonProperty("scale") BigDecimal scaleScore,

            @Schema(description = "텐션 점수 (0~100)", example = "78.0")
            @JsonProperty("tension") BigDecimal tensionScore,

            @Schema(description = "코드 진행 점수 (0~100)", example = "88.0")
            @JsonProperty("progression") BigDecimal progressionScore,

            @Schema(description = "보이싱 연결 점수 (0~100)", example = "76.5")
            @JsonProperty("voiceLeading") BigDecimal voiceLeadingScore
    ) {

        private static DomainScores from(Analysis analysis) {
            return new DomainScores(
                    analysis.getScaleScore(),
                    analysis.getTensionScore(),
                    analysis.getProgressionScore(),
                    analysis.getVoiceLeadingScore()
            );
        }
    }

    @Schema(description = "생성된 분석 리포트")
    public record Report(
            @Schema(description = "분석 리포트 ID", example = "91")
            Long analysisReportId,

            @Schema(description = "리포트 생성 방식 (LLM: AI 생성, RULE_BASED: 규칙 기반 대체 생성)", example = "LLM")
            ReportGenerationType generationType,

            @Schema(description = "LLM 호출 상태 (조회되는 리포트는 항상 SUCCESS)", example = "SUCCESS")
            LlmStatus llmStatus,

            @Schema(description = "리포트 본문 포맷", example = "MARKDOWN")
            ContentFormat contentFormat,

            @Schema(description = "리포트 본문", example = "## 총평\n8마디 구간에서 ...")
            String content,

            @Schema(description = "리포트 생성에 사용한 모델명. 규칙 기반 생성이면 null", example = "gemini-2.5-flash")
            String modelName,

            @Schema(description = "리포트 생성에 사용한 프롬프트 버전", example = "v1")
            String promptVersion,

            @Schema(description = "리포트 생성 시각", example = "2026-08-10T21:21:40")
            LocalDateTime createdAt,

            @Schema(description = "리포트 최종 수정 시각", example = "2026-08-10T21:21:40")
            LocalDateTime updatedAt
    ) {

        private static Report fromNullable(AnalysisReport analysisReport) {
            if (analysisReport == null) {
                return null;
            }

            return new Report(
                    analysisReport.getId(),
                    analysisReport.getGenerationType(),
                    analysisReport.getLlmStatus(),
                    analysisReport.getContentFormat(),
                    analysisReport.getContent(),
                    analysisReport.getModelName(),
                    analysisReport.getPromptVersion(),
                    analysisReport.getCreatedAt(),
                    analysisReport.getUpdatedAt()
            );
        }
    }
}
