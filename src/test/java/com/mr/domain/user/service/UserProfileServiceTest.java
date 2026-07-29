package com.mr.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mr.domain.learning.repository.UserLearningProgressRepository;
import com.mr.domain.statistics.repository.UserStatisticsRepository;
import com.mr.domain.subscriptions.entity.Subscription;
import com.mr.domain.subscriptions.repository.SubscriptionRepository;
import com.mr.domain.user.dto.req.UserProfileRequestDTO;
import com.mr.domain.user.dto.res.UserProfileResponseDTO;
import com.mr.domain.user.entity.Instrument;
import com.mr.domain.user.entity.Student;
import com.mr.domain.user.entity.StudentInstrument;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.entity.enums.TheoryLevel;
import com.mr.domain.user.entity.enums.UserRole;
import com.mr.domain.user.exception.InstrumentErrorStatus;
import com.mr.domain.user.exception.StudentErrorStatus;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.InstrumentRepository;
import com.mr.domain.user.repository.StudentInstrumentRepository;
import com.mr.domain.user.repository.StudentRepository;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.security.principal.CustomUserDetails;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private StudentInstrumentRepository studentInstrumentRepository;
    @Mock
    private InstrumentRepository instrumentRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private UserStatisticsRepository userStatisticsRepository;
    @Mock
    private UserLearningProgressRepository userLearningProgressRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.createFromOAuth("https://cdn.example.com/profile/default-profile.png");

        CustomUserDetails userDetails = new CustomUserDetails(USER_ID, UserRole.ROLE_STUDENT);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("registerProfile - 정상 온보딩 시 구독 등급은 요청값과 무관하게 항상 PRO로 저장된다")
    void registerProfile_alwaysUsesProTier() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(userRepository.existsByNicknameAndUserIdNot(any(), any())).willReturn(false);

        Student student = Student.create(user, TheoryLevel.INTERMEDIATE);
        given(studentRepository.saveAndFlush(any(Student.class))).willReturn(student);

        Instrument piano = Instrument.create("PIANO", "피아노");
        given(instrumentRepository.findByCode("PIANO")).willReturn(Optional.of(piano));

        given(subscriptionRepository.save(any(Subscription.class))).willAnswer(invocation -> invocation.getArgument(0));

        UserProfileRequestDTO.OnboardingRequest request =
                new UserProfileRequestDTO.OnboardingRequest("김뮤즈", TheoryLevel.INTERMEDIATE);

        UserProfileResponseDTO.OnboardingResponse response = userProfileService.registerProfile(request);

        assertThat(response.subscriptionTier()).isEqualTo("PRO");
        assertThat(user.getNickname()).isEqualTo("김뮤즈");
    }

    @Test
    @DisplayName("registerProfile - 이미 온보딩을 완료한 유저면 USER_409_02")
    void registerProfile_alreadyOnboarded() {
        user.updateNickname("이미가입함");
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        UserProfileRequestDTO.OnboardingRequest request =
                new UserProfileRequestDTO.OnboardingRequest("김뮤즈", TheoryLevel.INTERMEDIATE);

        assertThatThrownBy(() -> userProfileService.registerProfile(request))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getCode())
                .isEqualTo(UserErrorStatus.ONBOARDING_ALREADY_COMPLETED);

        verify(studentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("registerProfile - 동시 요청으로 닉네임이 먼저 선점되면(UNIQUE 위반) USER_409_01(닉네임 중복)로 변환된다")
    void registerProfile_nicknameRaceCondition_convertedToNicknameDuplicated() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(userRepository.existsByNicknameAndUserIdNot(any(), any())).willReturn(false);
        given(userRepository.saveAndFlush(any(User.class)))
                .willThrow(uniqueViolation("users_nickname_key"));

        UserProfileRequestDTO.OnboardingRequest request =
                new UserProfileRequestDTO.OnboardingRequest("김뮤즈", TheoryLevel.INTERMEDIATE);

        assertThatThrownBy(() -> userProfileService.registerProfile(request))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getCode())
                .isEqualTo(UserErrorStatus.NICKNAME_DUPLICATED);

        verify(studentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("registerProfile - 동시 요청으로 온보딩이 먼저 완료되면(Student UNIQUE 위반) USER_409_02로 변환된다")
    void registerProfile_studentRaceCondition_convertedToAlreadyCompleted() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(userRepository.existsByNicknameAndUserIdNot(any(), any())).willReturn(false);
        given(studentRepository.saveAndFlush(any(Student.class)))
                .willThrow(uniqueViolation("student_user_id_key"));

        UserProfileRequestDTO.OnboardingRequest request =
                new UserProfileRequestDTO.OnboardingRequest("김뮤즈", TheoryLevel.INTERMEDIATE);

        assertThatThrownBy(() -> userProfileService.registerProfile(request))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getCode())
                .isEqualTo(UserErrorStatus.ONBOARDING_ALREADY_COMPLETED);
    }

    @Test
    @DisplayName("registerProfile - 닉네임 위반이 아닌 다른 무결성 오류는 그대로 전파된다")
    void registerProfile_nonDuplicateIntegrityViolation_propagates() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(userRepository.existsByNicknameAndUserIdNot(any(), any())).willReturn(false);
        DataIntegrityViolationException notNullViolation =
                new DataIntegrityViolationException("not null violation");
        given(userRepository.saveAndFlush(any(User.class))).willThrow(notNullViolation);

        UserProfileRequestDTO.OnboardingRequest request =
                new UserProfileRequestDTO.OnboardingRequest("김뮤즈", TheoryLevel.INTERMEDIATE);

        assertThatThrownBy(() -> userProfileService.registerProfile(request))
                .isSameAs(notNullViolation);
    }

    @Test
    @DisplayName("registerProfile - PIANO 시드 데이터가 없으면 INSTRUMENT_500_01")
    void registerProfile_instrumentNotSeeded() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(userRepository.existsByNicknameAndUserIdNot(any(), any())).willReturn(false);

        Student student = Student.create(user, TheoryLevel.INTERMEDIATE);
        given(studentRepository.saveAndFlush(any(Student.class))).willReturn(student);
        given(instrumentRepository.findByCode("PIANO")).willReturn(Optional.empty());

        UserProfileRequestDTO.OnboardingRequest request =
                new UserProfileRequestDTO.OnboardingRequest("김뮤즈", TheoryLevel.INTERMEDIATE);

        assertThatThrownBy(() -> userProfileService.registerProfile(request))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getCode())
                .isEqualTo(InstrumentErrorStatus.INSTRUMENT_NOT_SEEDED);
    }

    @Test
    @DisplayName("getMyProfile - 통계의 completedLearningCount는 UserLearningProgressRepository 집계 결과를 그대로 사용한다")
    void getMyProfile_usesLearningProgressAggregationForCompletedLearningCount() {
        user.updateNickname("김뮤즈");
        Student student = Student.create(user, TheoryLevel.INTERMEDIATE);
        Instrument piano = Instrument.create("PIANO", "피아노");
        StudentInstrument primaryInstrument = StudentInstrument.createPrimary(student, piano);
        Subscription subscription = Subscription.create(
                user, "PRO", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(29));

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(studentRepository.findByUser(user)).willReturn(Optional.of(student));
        given(studentInstrumentRepository.findFirstByStudentAndPrimaryTrue(student))
                .willReturn(Optional.of(primaryInstrument));
        given(subscriptionRepository
                .findFirstByUserAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                        any(), any(), any()))
                .willReturn(Optional.of(subscription));
        given(userStatisticsRepository.findByUser_UserId(USER_ID)).willReturn(Optional.empty());
        given(userLearningProgressRepository.countDistinctCompletedLearningsByUserId(USER_ID)).willReturn(8L);

        UserProfileResponseDTO.ProfileResponse response = userProfileService.getMyProfile();

        assertThat(response.statistics().completedLearningCount()).isEqualTo(8L);
        verify(userLearningProgressRepository).countDistinctCompletedLearningsByUserId(USER_ID);
    }

    @Test
    @DisplayName("updateProfile - 온보딩 정보(student)가 없으면 STUDENT_404_01")
    void updateProfile_studentNotFound() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(studentRepository.findByUser(user)).willReturn(Optional.empty());

        UserProfileRequestDTO.UpdateRequest request =
                new UserProfileRequestDTO.UpdateRequest("새로운뮤즈", TheoryLevel.ADVANCED);

        assertThatThrownBy(() -> userProfileService.updateProfile(request))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getCode())
                .isEqualTo(StudentErrorStatus.STUDENT_NOT_FOUND);
    }

    @Test
    @DisplayName("updateProfile - 동시 요청으로 닉네임이 먼저 선점되면(UNIQUE 위반) USER_409_01로 변환된다")
    void updateProfile_nicknameRaceCondition_convertedToNicknameDuplicated() {
        user.updateNickname("기존닉네임");
        Student student = Student.create(user, TheoryLevel.INTERMEDIATE);

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(studentRepository.findByUser(user)).willReturn(Optional.of(student));
        given(userRepository.existsByNicknameAndUserIdNot(any(), any())).willReturn(false);
        given(userRepository.saveAndFlush(any(User.class)))
                .willThrow(uniqueViolation("users_nickname_key"));

        UserProfileRequestDTO.UpdateRequest request =
                new UserProfileRequestDTO.UpdateRequest("새로운뮤즈", TheoryLevel.ADVANCED);

        assertThatThrownBy(() -> userProfileService.updateProfile(request))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getCode())
                .isEqualTo(UserErrorStatus.NICKNAME_DUPLICATED);
    }

    @Test
    @DisplayName("updateProfile - nickname을 생략(null)하면 기존 닉네임을 유지하고 skillLevel만 변경한다")
    void updateProfile_partialUpdate_skillLevelOnly() {
        user.updateNickname("기존닉네임");
        Student student = Student.create(user, TheoryLevel.BEGINNER);

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(studentRepository.findByUser(user)).willReturn(Optional.of(student));

        UserProfileRequestDTO.UpdateRequest request =
                new UserProfileRequestDTO.UpdateRequest(null, TheoryLevel.ADVANCED);

        UserProfileResponseDTO.UpdateResponse response = userProfileService.updateProfile(request);

        assertThat(response.nickname()).isEqualTo("기존닉네임");
        assertThat(response.skillLevel()).isEqualTo(TheoryLevel.ADVANCED);
        verify(userRepository, never()).existsByNicknameAndUserIdNot(any(), any());
    }

    @Test
    @DisplayName("updateProfile - skillLevel을 생략(null)하면 기존 숙련도를 유지하고 nickname만 변경한다")
    void updateProfile_partialUpdate_nicknameOnly() {
        user.updateNickname("기존닉네임");
        Student student = Student.create(user, TheoryLevel.BEGINNER);

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(studentRepository.findByUser(user)).willReturn(Optional.of(student));
        given(userRepository.existsByNicknameAndUserIdNot(any(), any())).willReturn(false);

        UserProfileRequestDTO.UpdateRequest request =
                new UserProfileRequestDTO.UpdateRequest("새로운뮤즈", null);

        UserProfileResponseDTO.UpdateResponse response = userProfileService.updateProfile(request);

        assertThat(response.nickname()).isEqualTo("새로운뮤즈");
        assertThat(response.skillLevel()).isEqualTo(TheoryLevel.BEGINNER);
    }

    private static DataIntegrityViolationException uniqueViolation(String constraintName) {
        SQLException sqlException = new SQLException("duplicate key value violates unique constraint", "23505");
        ConstraintViolationException constraintViolationException =
                new ConstraintViolationException("duplicate key", sqlException, constraintName);
        return new DataIntegrityViolationException("duplicate key", constraintViolationException);
    }
}
