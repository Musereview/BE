package com.mr.domain.home.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.learning.dto.res.LearningHomeResponseDTO;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.user.entity.enums.TheoryLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;

@Schema(description = "홈 화면 요약 조회 응답")
public record HomeResponseDTO(
        UserSummary user,
        Streak streak,
        PracticeSummary practiceSummary,

        @Schema(description = "진행 중인 학습(재도전 포함). 이어갈 단계가 없으면(진행 기록이 아예 없거나, 최근 패키지들이 전부 100% 완료) null")
        LearningSummary currentLearning,

        @Schema(description = "추천 학습 목록")
        List<RecommendedLearning> recommendedLearnings,

        @Schema(description = "최근 완료된 연습 5건, 최신순")
        List<RecentPlaying> recentPlayings
) {

    @Schema(description = "사용자 프로필 요약")
    public record UserSummary(
            @Schema(description = "사용자 ID", example = "1")
            Long userId,

            @Schema(description = "닉네임", example = "김뮤즈")
            String nickname,

            @Schema(description = "프로필 이미지 URL", example = "https://cdn.example.com/profile/1.png")
            String profileImgUrl,

            @Schema(description = "이론 레벨. Student 정보 없으면 null", example = "INTERMEDIATE")
            TheoryLevel skillLevel,

            @Schema(description = "주 악기 코드. Student 정보 없으면 null", example = "KEYBOARD")
            String instrumentType
    ) {
    }

    @Schema(description = "연속 출석 현황")
    public record Streak(
            @Schema(description = "현재 연속 학습 일수, 조회 기간 상한 없음", example = "4")
            int currentDays,

            @Schema(description = "홈 상단 노출 문구", example = "4일 연속 학습 중이에요!")
            String message,

            @Schema(description = "이번 주 월~일 출석 상태 7개 고정")
            List<DayAttendance> weeklyAttendance
    ) {

        @Schema(description = "요일별 출석 상태")
        public record DayAttendance(
                @Schema(description = "요일 코드")
                DayOfWeekCode dayOfWeek,

                @Schema(description = "요일 한글 라벨", example = "월")
                String label,

                @Schema(description = "출석 상태")
                AttendanceStatus status
        ) {
        }
    }

    @Schema(description = "요일 코드")
    public enum DayOfWeekCode {
        MON, TUE, WED, THU, FRI, SAT, SUN;

        public static DayOfWeekCode from(DayOfWeek dayOfWeek) {
            return DayOfWeekCode.valueOf(dayOfWeek.name().substring(0, 3));
        }
    }

    @Schema(description = "출석 상태. COMPLETED(과거 연습함), TODAY_COMPLETED(오늘 연습함), MISSED(과거 연습 안 함), EMPTY(미래이거나 오늘 아직 연습 전)")
    public enum AttendanceStatus {
        COMPLETED, TODAY_COMPLETED, MISSED, EMPTY
    }

    @Schema(description = "주/월간 연습 시간 요약")
    public record PracticeSummary(
            @Schema(description = "이번 주(월~오늘) 연습 시간 합, 시간 단위 버림", example = "21")
            int weeklyPracticeHours,

            @Schema(description = "이번 달(1일~오늘) 연습 시간 합, 시간 단위 버림", example = "60")
            int monthlyPracticeHours,

            @Schema(description = "이번 달 라벨", example = "6월")
            String monthLabel
    ) {
    }

    @Schema(description = "진행 중인 학습")
    public record LearningSummary(
            @Schema(description = "패키지 ID", example = "5")
            Long learningId,

            @Schema(description = "패키지 제목", example = "Tension Notes")
            String title,

            @Schema(description = "마지막으로 학습한 단계의 제목", example = "11th 텐션 노트 활용하기")
            String subtitle,

            @Schema(description = "패키지 난이도", example = "ADVANCED")
            String level,

            @Schema(description = "패키지 진행률(%), 0~99", example = "10")
            int progressRate,

            @Schema(description = "[이어서 학습하기] 클릭 시 이동할 단계 ID", example = "13")
            Long nextStepId
    ) {

        public static LearningSummary from(LearningHomeResponseDTO.CurrentLearning currentLearning) {
            if (currentLearning == null) {
                return null;
            }

            return new LearningSummary(
                    currentLearning.learningId(),
                    currentLearning.title(),
                    currentLearning.stepTitle(),
                    currentLearning.difficulty(),
                    currentLearning.progressRate(),
                    currentLearning.nextStepId()
            );
        }
    }

    @Schema(description = "추천 학습")
    public record RecommendedLearning(
            @Schema(description = "패키지 ID", example = "5")
            Long learningId,

            @Schema(description = "패키지 제목", example = "Tension Notes")
            String title,

            @Schema(description = "추천 단계 제목", example = "13th 텐션 노트 활용하기")
            String subtitle,

            @Schema(description = "패키지 난이도", example = "ADVANCED")
            String level,

            @Schema(description = "이동할 단계 ID", example = "14")
            Long nextStepId
    ) {

        public static RecommendedLearning from(LearningHomeResponseDTO.RecommendedLearning recommended) {
            return new RecommendedLearning(
                    recommended.learningId(),
                    recommended.title(),
                    recommended.subtitle(),
                    recommended.level(),
                    recommended.nextStepId()
            );
        }
    }

    @Schema(description = "최근 연습")
    public record RecentPlaying(
            @Schema(description = "연주 세션 ID", example = "31")
            Long playingId,

            @Schema(description = "백킹트랙 제목. 백킹트랙 없으면 null", example = "Jazz Standard Practice")
            String title,

            @Schema(description = "백킹트랙 장르. 백킹트랙 없으면 null", example = "JAZZ")
            String genre,

            @Schema(description = "백킹트랙 조성. 백킹트랙 없으면 null", example = "C Major")
            String key,

            @Schema(description = "BPM", example = "120")
            Integer bpm,

            @Schema(description = "연습 종료 시각 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
            Instant playedAt,

            @Schema(description = "상대 시간 표기", example = "오늘")
            String relativeTime,

            @Schema(description = "연습 시간(분)", example = "12")
            Integer durationMinutes
    ) {

        public static RecentPlaying of(Playing playing, String relativeTime) {
            BackingTrack backingTrack = playing.getBackingTrack();

            return new RecentPlaying(
                    playing.getId(),
                    backingTrack != null ? backingTrack.getTitle() : null,
                    backingTrack != null ? backingTrack.getGenre() : null,
                    backingTrack != null ? backingTrack.getKeySignature() : null,
                    playing.getBpm(),
                    playing.getEndedAt(),
                    relativeTime,
                    playing.getDurationSec() != null ? playing.getDurationSec() / 60 : null
            );
        }
    }
}