package com.mr.domain.home.service;

import com.mr.domain.home.dto.res.HomeResponseDTO;
import com.mr.domain.home.dto.res.HomeResponseDTO.AttendanceStatus;
import com.mr.domain.home.dto.res.HomeResponseDTO.DayOfWeekCode;
import com.mr.domain.home.dto.res.HomeResponseDTO.LearningSummary;
import com.mr.domain.home.dto.res.HomeResponseDTO.PracticeSummary;
import com.mr.domain.home.dto.res.HomeResponseDTO.RecentPlaying;
import com.mr.domain.home.dto.res.HomeResponseDTO.RecommendedLearning;
import com.mr.domain.home.dto.res.HomeResponseDTO.Streak;
import com.mr.domain.home.dto.res.HomeResponseDTO.Streak.DayAttendance;
import com.mr.domain.home.dto.res.HomeResponseDTO.UserSummary;
import com.mr.domain.learning.dto.res.LearningHomeResponseDTO;
import com.mr.domain.learning.service.LearningService;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import com.mr.domain.playing.repository.PlayingRepository;
import com.mr.domain.user.entity.Student;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.entity.enums.TheoryLevel;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.StudentInstrumentRepository;
import com.mr.domain.user.repository.StudentRepository;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.util.RelativeDateFormatter;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private static final int RECENT_PLAYINGS_LIMIT = 5;
    private static final int STREAK_LOOKBACK_DAYS = 60;
    private static final int SECONDS_PER_HOUR = 3600;

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StudentInstrumentRepository studentInstrumentRepository;
    private final PlayingRepository playingRepository;
    private final LearningService learningService;

    public HomeResponseDTO getHome(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        Set<LocalDate> practiceDates = fetchPracticeDates(userId);

        LocalDateTime since = LocalDateTime.now().minusDays(STREAK_LOOKBACK_DAYS);
        List<Playing> recentCompleted =
                playingRepository.findByUserAndStatusSince(userId, PlayingStatus.COMPLETED, since);

        LearningHomeResponseDTO.CurrentLearning currentLearning = learningService.getCurrentLearning(userId);

        return new HomeResponseDTO(
                buildUserSummary(user),
                buildStreak(practiceDates),
                buildPracticeSummary(recentCompleted),
                LearningSummary.from(currentLearning),
                buildRecommendedLearnings(userId, currentLearning),
                buildRecentPlayings(userId)
        );
    }

    private List<RecommendedLearning> buildRecommendedLearnings(
            Long userId, LearningHomeResponseDTO.CurrentLearning currentLearning) {
        Long excludeStepId = currentLearning != null ? currentLearning.nextStepId() : null;

        return learningService.getRecommendedLearnings(userId, excludeStepId).stream()
                .map(RecommendedLearning::from)
                .toList();
    }

    // 연속 출석일수는 기간 상한이 없어야 하므로 별도로 전체 기간 날짜만 조회
    private Set<LocalDate> fetchPracticeDates(Long userId) {
        return new HashSet<>(
                playingRepository.findDistinctEndedDatesByUserAndStatus(userId, PlayingStatus.COMPLETED));
    }

    private UserSummary buildUserSummary(User user) {
        Student student = studentRepository.findByUser(user).orElse(null);

        TheoryLevel skillLevel = student != null ? student.getTheoryLevel() : null;
        String instrumentType = student == null ? null
                : studentInstrumentRepository.findFirstByStudentAndPrimaryTrue(student)
                        .map(si -> si.getInstrument().getCode())
                        .orElse(null);

        return new UserSummary(user.getUserId(), user.getNickname(), user.getProfileImgUrl(),
                skillLevel, instrumentType);
    }

    private Streak buildStreak(Set<LocalDate> practiceDates) {
        int currentDays = computeCurrentStreak(practiceDates);

        return new Streak(currentDays, buildStreakMessage(currentDays), buildWeeklyAttendance(practiceDates));
    }

    private int computeCurrentStreak(Set<LocalDate> practiceDates) {
        LocalDate today = LocalDate.now();
        LocalDate cursor = practiceDates.contains(today) ? today : today.minusDays(1);

        int streak = 0;
        while (practiceDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private String buildStreakMessage(int currentDays) {
        if (currentDays <= 0) {
            return "오늘부터 연습을 시작해보세요!";
        }
        return currentDays + "일 연속 학습 중이에요!";
    }

    private List<DayAttendance> buildWeeklyAttendance(Set<LocalDate> practiceDates) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);

        List<DayAttendance> weeklyAttendance = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            weeklyAttendance.add(new DayAttendance(
                    DayOfWeekCode.from(day.getDayOfWeek()),
                    toKoreanLabel(day.getDayOfWeek()),
                    resolveStatus(day, today, practiceDates)
            ));
        }
        return weeklyAttendance;
    }

    private AttendanceStatus resolveStatus(LocalDate day, LocalDate today, Set<LocalDate> practiceDates) {
        if (day.isAfter(today)) {
            return AttendanceStatus.EMPTY;
        }
        boolean practiced = practiceDates.contains(day);
        if (day.isEqual(today)) {
            // 오늘 아직 연습 전이면 명세에 없는 케이스라 미래 요일과 동일하게 EMPTY 처리
            return practiced ? AttendanceStatus.TODAY_COMPLETED : AttendanceStatus.EMPTY;
        }
        return practiced ? AttendanceStatus.COMPLETED : AttendanceStatus.MISSED;
    }

    private String toKoreanLabel(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
    }

    private PracticeSummary buildPracticeSummary(List<Playing> recentCompleted) {
        LocalDate today = LocalDate.now();
        LocalDateTime weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

        int weeklySeconds = sumDurationSince(recentCompleted, weekStart);
        int monthlySeconds = sumDurationSince(recentCompleted, monthStart);

        return new PracticeSummary(
                weeklySeconds / SECONDS_PER_HOUR,
                monthlySeconds / SECONDS_PER_HOUR,
                today.getMonthValue() + "월"
        );
    }

    private int sumDurationSince(List<Playing> playings, LocalDateTime since) {
        return playings.stream()
                .filter(p -> p.getEndedAt() != null && !p.getEndedAt().isBefore(since))
                .mapToInt(p -> p.getDurationSec() != null ? p.getDurationSec() : 0)
                .sum();
    }

    private List<RecentPlaying> buildRecentPlayings(Long userId) {
        Slice<Playing> slice = playingRepository.findPlayingsByUserAndStatus(
                userId, PlayingStatus.COMPLETED, PageRequest.of(0, RECENT_PLAYINGS_LIMIT));

        return slice.getContent().stream()
                .map(playing -> RecentPlaying.of(playing, RelativeDateFormatter.format(playing.getEndedAt())))
                .toList();
    }
}
