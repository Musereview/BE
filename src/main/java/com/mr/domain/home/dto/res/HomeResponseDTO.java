package com.mr.domain.home.dto.res;

import com.mr.domain.backingTrack.entity.BackingTrack;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.user.entity.enums.TheoryLevel;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

public record HomeResponseDTO(
        UserSummary user,
        Streak streak,
        PracticeSummary practiceSummary,
        LearningSummary currentLearning,
        List<RecommendedLearning> recommendedLearnings,
        List<RecentPlaying> recentPlayings
) {

    public record UserSummary(
            Long userId,
            String nickname,
            String profileImgUrl,
            TheoryLevel skillLevel,
            String instrumentType
    ) {
    }

    public record Streak(
            int currentDays,
            String message,
            List<DayAttendance> weeklyAttendance
    ) {

        public record DayAttendance(
                DayOfWeekCode dayOfWeek,
                String label,
                AttendanceStatus status
        ) {
        }
    }

    public enum DayOfWeekCode {
        MON, TUE, WED, THU, FRI, SAT, SUN;

        public static DayOfWeekCode from(DayOfWeek dayOfWeek) {
            return DayOfWeekCode.valueOf(dayOfWeek.name().substring(0, 3));
        }
    }

    public enum AttendanceStatus {
        COMPLETED, TODAY_COMPLETED, MISSED, EMPTY
    }

    public record PracticeSummary(
            int weeklyPracticeHours,
            int monthlyPracticeHours,
            String monthLabel
    ) {
    }

    // TODO: currentLearning 소스(UserLearningProgress 진행률 계산 방식) 확정 전까지 항상 null
    public record LearningSummary(
            Long learningId,
            String title,
            String subtitle,
            String level,
            int progressRate
    ) {
    }

    // TODO: 추천 학습 알고리즘 확정 전까지 항상 빈 배열
    public record RecommendedLearning(
            Long learningId,
            String title,
            List<String> levels,
            String description
    ) {
    }

    public record RecentPlaying(
            Long playingId,
            String title,
            String genre,
            String key,
            Integer bpm,
            LocalDateTime playedAt,
            String relativeTime,
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
