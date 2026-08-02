package com.mr.domain.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.mr.domain.home.dto.res.HomeResponseDTO;
import com.mr.domain.home.dto.res.HomeResponseDTO.AttendanceStatus;
import com.mr.domain.home.dto.res.HomeResponseDTO.DayOfWeekCode;
import com.mr.domain.home.dto.res.HomeResponseDTO.Streak.DayAttendance;
import com.mr.domain.learning.dto.res.LearningHomeResponseDTO;
import com.mr.domain.learning.service.LearningService;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import com.mr.domain.playing.repository.PlayingRepository;
import com.mr.domain.user.entity.Instrument;
import com.mr.domain.user.entity.Student;
import com.mr.domain.user.entity.StudentInstrument;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.entity.enums.TheoryLevel;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.StudentInstrumentRepository;
import com.mr.domain.user.repository.StudentRepository;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentInstrumentRepository studentInstrumentRepository;

    @Mock
    private PlayingRepository playingRepository;

    @Mock
    private LearningService learningService;

    private HomeService homeService;

    @BeforeEach
    void setUp() {
        homeService = new HomeService(
                userRepository, studentRepository, studentInstrumentRepository, playingRepository, learningService);
    }

    private void stubBaseline(Long userId) {
        User user = mock(User.class);
        lenient().when(user.getUserId()).thenReturn(userId);
        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        lenient().when(studentRepository.findByUser(user)).thenReturn(Optional.empty());
        lenient().when(playingRepository.findDistinctEndedDatesByUserAndStatus(anyLong(), any())).thenReturn(List.of());
        lenient().when(playingRepository.findByUserAndStatusSince(anyLong(), any(), any())).thenReturn(List.of());
        lenient().when(playingRepository.findPlayingsByUserAndStatus(anyLong(), any(), any()))
                .thenReturn(new SliceImpl<>(List.of()));
        lenient().when(learningService.getCurrentLearning(anyLong())).thenReturn(null);
    }

    private Playing mockPlaying(LocalDateTime endedAt, Integer durationSec) {
        Playing playing = mock(Playing.class);
        lenient().when(playing.getId()).thenReturn(1L);
        lenient().when(playing.getEndedAt()).thenReturn(endedAt);
        lenient().when(playing.getDurationSec()).thenReturn(durationSec);
        lenient().when(playing.getBpm()).thenReturn(120);
        lenient().when(playing.getBackingTrack()).thenReturn(null);
        return playing;
    }

    @Test
    @DisplayName("getHome - 존재하지 않는 사용자면 404")
    void getHome_userNotFound_throws404() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> homeService.getHome(1L))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorStatus.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("getHome - Student/주악기가 있으면 skillLevel/instrumentType을 채운다")
    void getHome_withStudentAndInstrument_fillsSkillLevelAndInstrument() {
        stubBaseline(1L);

        Student student = mock(Student.class);
        given(student.getTheoryLevel()).willReturn(TheoryLevel.INTERMEDIATE);
        given(studentRepository.findByUser(any(User.class))).willReturn(Optional.of(student));

        Instrument instrument = mock(Instrument.class);
        given(instrument.getCode()).willReturn("KEYBOARD");
        StudentInstrument studentInstrument = mock(StudentInstrument.class);
        given(studentInstrument.getInstrument()).willReturn(instrument);
        given(studentInstrumentRepository.findFirstByStudentAndPrimaryTrue(student))
                .willReturn(Optional.of(studentInstrument));

        HomeResponseDTO response = homeService.getHome(1L);

        assertThat(response.user().skillLevel()).isEqualTo(TheoryLevel.INTERMEDIATE);
        assertThat(response.user().instrumentType()).isEqualTo("KEYBOARD");
    }

    @Test
    @DisplayName("getHome - Student가 없으면 skillLevel/instrumentType은 null이다")
    void getHome_noStudent_skillLevelAndInstrumentAreNull() {
        stubBaseline(1L);

        HomeResponseDTO response = homeService.getHome(1L);

        assertThat(response.user().skillLevel()).isNull();
        assertThat(response.user().instrumentType()).isNull();
    }

    @Test
    @DisplayName("getHome - 오늘까지 3일 연속 연습했으면 currentDays는 3이다")
    void getHome_threeConsecutiveDaysIncludingToday_currentDaysIsThree() {
        stubBaseline(1L);

        LocalDate today = LocalDate.now();
        given(playingRepository.findDistinctEndedDatesByUserAndStatus(1L, PlayingStatus.COMPLETED)).willReturn(List.of(
                today, today.minusDays(1), today.minusDays(2), today.minusDays(5) // 연속 끊김
        ));

        HomeResponseDTO response = homeService.getHome(1L);

        assertThat(response.streak().currentDays()).isEqualTo(3);
    }

    @Test
    @DisplayName("getHome - 61일 연속 연습해도 스트릭 조회에는 기간 상한이 없다")
    void getHome_longStreak_notCappedByLookbackWindow() {
        stubBaseline(1L);

        LocalDate today = LocalDate.now();
        List<LocalDate> endedDates = java.util.stream.IntStream.range(0, 65)
                .mapToObj(today::minusDays)
                .toList();
        given(playingRepository.findDistinctEndedDatesByUserAndStatus(1L, PlayingStatus.COMPLETED)).willReturn(endedDates);

        HomeResponseDTO response = homeService.getHome(1L);

        assertThat(response.streak().currentDays()).isEqualTo(65);
    }

    @Test
    @DisplayName("getHome - 오늘 연습을 안 했어도 어제까지 연속이면 currentDays에 반영된다")
    void getHome_noPracticeToday_countsFromYesterday() {
        stubBaseline(1L);

        LocalDate yesterday = LocalDate.now().minusDays(1);
        given(playingRepository.findDistinctEndedDatesByUserAndStatus(1L, PlayingStatus.COMPLETED))
                .willReturn(List.of(yesterday));

        HomeResponseDTO response = homeService.getHome(1L);

        assertThat(response.streak().currentDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("getHome - 같은 날짜가 중복 반환돼도 연속일수는 하루로만 카운트된다")
    void getHome_duplicateDateFromRepository_countsOnce() {
        stubBaseline(1L);

        LocalDate today = LocalDate.now();
        given(playingRepository.findDistinctEndedDatesByUserAndStatus(1L, PlayingStatus.COMPLETED))
                .willReturn(List.of(today, today, today));

        HomeResponseDTO response = homeService.getHome(1L);

        assertThat(response.streak().currentDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("getHome - weeklyAttendance는 월요일부터 일요일까지 7개이고 미래 요일은 EMPTY다")
    void getHome_weeklyAttendance_hasSevenDaysAndFutureDaysAreEmpty() {
        stubBaseline(1L);

        HomeResponseDTO response = homeService.getHome(1L);

        List<DayAttendance> weeklyAttendance = response.streak().weeklyAttendance();
        assertThat(weeklyAttendance).hasSize(7);
        assertThat(weeklyAttendance.get(0).dayOfWeek()).isEqualTo(DayOfWeekCode.MON);
        assertThat(weeklyAttendance.get(6).dayOfWeek()).isEqualTo(DayOfWeekCode.SUN);

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        for (int i = 0; i < weeklyAttendance.size(); i++) {
            LocalDate day = weekStart.plusDays(i);
            if (day.isAfter(today)) {
                assertThat(weeklyAttendance.get(i).status()).isEqualTo(AttendanceStatus.EMPTY);
            }
        }
    }

    @Test
    @DisplayName("getHome - practiceSummary는 이번 주/이번 달 연습 시간을 시간 단위로 합산한다")
    void getHome_practiceSummary_sumsDurationInHours() {
        stubBaseline(1L);

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<Playing> recent = List.of(
                mockPlaying(todayStart, 3600),
                mockPlaying(todayStart.plusHours(2), 3600)
        );
        given(playingRepository.findByUserAndStatusSince(anyLong(), any(), any())).willReturn(recent);

        HomeResponseDTO response = homeService.getHome(1L);

        assertThat(response.practiceSummary().weeklyPracticeHours()).isEqualTo(2);
        assertThat(response.practiceSummary().monthlyPracticeHours()).isEqualTo(2);
    }

    @Test
    @DisplayName("getHome - 이번 주 시작 직전 연습 시간은 weeklyPracticeHours에서 제외된다")
    void getHome_practiceSummary_excludesRecordsBeforeWeekStart() {
        stubBaseline(1L);

        LocalDateTime weekStart = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
        List<Playing> recent = List.of(
                mockPlaying(weekStart, 3600),
                mockPlaying(weekStart.minusSeconds(1), 3600)
        );
        given(playingRepository.findByUserAndStatusSince(anyLong(), any(), any())).willReturn(recent);

        HomeResponseDTO response = homeService.getHome(1L);

        assertThat(response.practiceSummary().weeklyPracticeHours()).isEqualTo(1);
    }

    @Test
    @DisplayName("getHome - LearningService에 진행 중인 학습이 있으면 currentLearning을 채운다")
    void getHome_hasCurrentLearning_fillsCurrentLearning() {
        stubBaseline(1L);

        com.mr.domain.learning.entity.Learning learning = mock(com.mr.domain.learning.entity.Learning.class);
        given(learning.getId()).willReturn(5L);
        given(learning.getTitle()).willReturn("Tension Notes");
        given(learning.getDifficulty()).willReturn(com.mr.domain.learning.entity.enums.LearningDifficulty.ADVANCED);

        LearningHomeResponseDTO.CurrentLearning currentLearning =
                LearningHomeResponseDTO.CurrentLearning.of(learning, "11th 텐션 노트 활용하기", 10, 13L);
        given(learningService.getCurrentLearning(1L)).willReturn(currentLearning);

        HomeResponseDTO response = homeService.getHome(1L);

        assertThat(response.currentLearning()).isNotNull();
        assertThat(response.currentLearning().learningId()).isEqualTo(5L);
        assertThat(response.currentLearning().subtitle()).isEqualTo("11th 텐션 노트 활용하기");
        assertThat(response.currentLearning().level()).isEqualTo("ADVANCED");
        assertThat(response.currentLearning().progressRate()).isEqualTo(10);
        assertThat(response.currentLearning().nextStepId()).isEqualTo(13L);
    }

    @Test
    @DisplayName("getHome - 진행 중인 학습이 없으면 currentLearning은 null이다")
    void getHome_noCurrentLearning_currentLearningIsNull() {
        stubBaseline(1L);

        HomeResponseDTO response = homeService.getHome(1L);

        assertThat(response.currentLearning()).isNull();
    }

    @Test
    @DisplayName("getHome - LearningService가 추천 학습을 반환하면 recommendedLearnings에 그대로 매핑한다")
    void getHome_hasRecommendedLearnings_mapsToResponse() {
        stubBaseline(1L);

        LearningHomeResponseDTO.RecommendedLearning recommended =
                new LearningHomeResponseDTO.RecommendedLearning(5L, "Tension Notes", "13th 텐션 노트 활용하기", "ADVANCED", 14L);
        given(learningService.getRecommendedLearnings(1L, null)).willReturn(List.of(recommended));

        HomeResponseDTO response = homeService.getHome(1L);

        assertThat(response.recommendedLearnings()).hasSize(1);
        assertThat(response.recommendedLearnings().get(0).learningId()).isEqualTo(5L);
        assertThat(response.recommendedLearnings().get(0).subtitle()).isEqualTo("13th 텐션 노트 활용하기");
        assertThat(response.recommendedLearnings().get(0).nextStepId()).isEqualTo(14L);
    }

    @Test
    @DisplayName("getHome - currentLearning이 있으면 그 nextStepId를 추천 학습 제외 대상으로 전달한다")
    void getHome_hasCurrentLearning_passesNextStepIdAsExcludeToRecommendedLearnings() {
        stubBaseline(1L);

        com.mr.domain.learning.entity.Learning learning = mock(com.mr.domain.learning.entity.Learning.class);
        given(learning.getId()).willReturn(5L);
        given(learning.getTitle()).willReturn("Tension Notes");
        given(learning.getDifficulty()).willReturn(com.mr.domain.learning.entity.enums.LearningDifficulty.ADVANCED);

        LearningHomeResponseDTO.CurrentLearning currentLearning =
                LearningHomeResponseDTO.CurrentLearning.of(learning, "11th 텐션 노트 활용하기", 10, 13L);
        given(learningService.getCurrentLearning(1L)).willReturn(currentLearning);
        given(learningService.getRecommendedLearnings(1L, 13L)).willReturn(List.of());

        homeService.getHome(1L);

        verify(learningService).getRecommendedLearnings(1L, 13L);
    }

    @Test
    @DisplayName("getHome - LearningService가 추천 학습이 없다고 하면 빈 배열이다")
    void getHome_noRecommendedLearnings_returnsEmptyList() {
        stubBaseline(1L);

        HomeResponseDTO response = homeService.getHome(1L);

        assertThat(response.recommendedLearnings()).isEmpty();
    }

    @Test
    @DisplayName("getHome - recentPlayings는 findPlayingsByUserAndStatus 결과를 매핑한다")
    void getHome_recentPlayings_mapsFromRepository() {
        stubBaseline(1L);

        Playing playing = mockPlaying(LocalDateTime.now(), 600);
        given(playingRepository.findPlayingsByUserAndStatus(1L, PlayingStatus.COMPLETED, PageRequest.of(0, 5)))
                .willReturn(new SliceImpl<>(List.of(playing)));

        HomeResponseDTO response = homeService.getHome(1L);

        assertThat(response.recentPlayings()).hasSize(1);
        assertThat(response.recentPlayings().get(0).durationMinutes()).isEqualTo(10);
    }
}
