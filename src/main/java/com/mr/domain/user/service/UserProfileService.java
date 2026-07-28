package com.mr.domain.user.service;

import com.mr.domain.learning.repository.UserLearningProgressRepository;
import com.mr.domain.statistics.entity.UserStatistics;
import com.mr.domain.statistics.repository.UserStatisticsRepository;
import com.mr.domain.subscriptions.entity.Subscription;
import com.mr.domain.subscriptions.entity.enums.SubscriptionTier;
import com.mr.domain.subscriptions.exception.SubscriptionErrorStatus;
import com.mr.domain.subscriptions.repository.SubscriptionRepository;
import com.mr.domain.user.dto.req.UserProfileRequestDTO;
import com.mr.domain.user.dto.res.UserProfileResponseDTO;
import com.mr.domain.user.entity.Instrument;
import com.mr.domain.user.entity.Student;
import com.mr.domain.user.entity.StudentInstrument;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.exception.InstrumentErrorStatus;
import com.mr.domain.user.exception.StudentErrorStatus;
import com.mr.domain.user.exception.StudentInstrumentErrorStatus;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.InstrumentRepository;
import com.mr.domain.user.repository.StudentInstrumentRepository;
import com.mr.domain.user.repository.StudentRepository;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.security.SecurityUtil;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileService {

    private static final String PIANO_CODE = "PIANO";
    private static final int SUBSCRIPTION_PERIOD_DAYS = 30;
    private static final String POSTGRES_UNIQUE_VIOLATION_SQL_STATE = "23505";

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StudentInstrumentRepository studentInstrumentRepository;
    private final InstrumentRepository instrumentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserStatisticsRepository userStatisticsRepository;
    private final UserLearningProgressRepository userLearningProgressRepository;

    public UserProfileResponseDTO.ProfileResponse getMyProfile() {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = getUser(userId);
        Student student = getStudent(user);

        String instrumentType = studentInstrumentRepository.findFirstByStudentAndPrimaryTrue(student)
                .map(StudentInstrument::getInstrument)
                .map(Instrument::getCode)
                .orElseThrow(() -> new GeneralException(StudentInstrumentErrorStatus.PRIMARY_INSTRUMENT_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        String subscriptionTier = subscriptionRepository
                .findFirstByUserAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(user, now, now)
                .map(Subscription::getTier)
                .orElseThrow(() -> new GeneralException(SubscriptionErrorStatus.ACTIVE_SUBSCRIPTION_NOT_FOUND));

        return UserProfileResponseDTO.ProfileResponse.builder()
                .nickname(user.getNickname())
                .profileImgUrl(user.getProfileImgUrl())
                .instrumentType(instrumentType)
                .skillLevel(student.getTheoryLevel())
                .subscriptionTier(subscriptionTier)
                .statistics(buildStatistics(userId))
                .build();
    }

    @Transactional
    public UserProfileResponseDTO.OnboardingResponse registerProfile(UserProfileRequestDTO.OnboardingRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = getUser(userId);

        if (user.isOnboardingCompleted()) {
            throw new GeneralException(UserErrorStatus.ONBOARDING_ALREADY_COMPLETED);
        }

        String trimmedNickname = trimNickname(request.nickname());
        ensureNicknameNotTaken(user.getUserId(), trimmedNickname);
        user.updateNickname(trimmedNickname);

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateKeyViolation(e)) {
                // 동시에 들어온 다른 요청이 먼저 같은 닉네임을 선점한 경우(User.nickname unique 제약 위반)
                throw new GeneralException(UserErrorStatus.NICKNAME_DUPLICATED);
            }
            throw e;
        }

        Student student;
        try {
            student = studentRepository.saveAndFlush(Student.create(user, request.skillLevel()));
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateKeyViolation(e)) {
                // 동시에 들어온 다른 요청이 먼저 온보딩을 완료한 경우(Student.user_id unique 제약 위반)
                throw new GeneralException(UserErrorStatus.ONBOARDING_ALREADY_COMPLETED);
            }
            throw e;
        }

        Instrument piano = instrumentRepository.findByCode(PIANO_CODE)
                .orElseThrow(() -> new GeneralException(InstrumentErrorStatus.INSTRUMENT_NOT_SEEDED));
        studentInstrumentRepository.save(StudentInstrument.createPrimary(student, piano));

        LocalDateTime now = LocalDateTime.now();
        Subscription subscription = subscriptionRepository.save(
                Subscription.create(user, SubscriptionTier.PRO.name(), now, now.plusDays(SUBSCRIPTION_PERIOD_DAYS)));

        return UserProfileResponseDTO.OnboardingResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .skillLevel(student.getTheoryLevel())
                .instrumentType(piano.getCode())
                .subscriptionTier(subscription.getTier())
                .profileImgUrl(user.getProfileImgUrl())
                .build();
    }

    @Transactional
    public UserProfileResponseDTO.UpdateResponse updateProfile(UserProfileRequestDTO.UpdateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = getUser(userId);
        Student student = getStudent(user);

        if (request.nickname() != null) {
            String trimmedNickname = trimNickname(request.nickname());
            ensureNicknameNotTaken(user.getUserId(), trimmedNickname);
            user.updateNickname(trimmedNickname);
        }
        if (request.skillLevel() != null) {
            student.updateTheoryLevel(request.skillLevel());
        }

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateKeyViolation(e)) {
                // 동시에 들어온 다른 요청이 먼저 같은 닉네임을 선점한 경우(User.nickname unique 제약 위반)
                throw new GeneralException(UserErrorStatus.NICKNAME_DUPLICATED);
            }
            throw e;
        }

        return UserProfileResponseDTO.UpdateResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .skillLevel(student.getTheoryLevel())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));
    }

    private Student getStudent(User user) {
        return studentRepository.findByUser(user)
                .orElseThrow(() -> new GeneralException(StudentErrorStatus.STUDENT_NOT_FOUND));
    }

    private boolean isDuplicateKeyViolation(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException constraintViolationException
                && POSTGRES_UNIQUE_VIOLATION_SQL_STATE.equals(constraintViolationException.getSQLState());
    }

    private String trimNickname(String nickname) {
        return nickname == null ? null : nickname.trim();
    }

    private void ensureNicknameNotTaken(Long userId, String trimmedNickname) {
        if (userRepository.existsByNicknameAndUserIdNot(trimmedNickname, userId)) {
            throw new GeneralException(UserErrorStatus.NICKNAME_DUPLICATED);
        }
    }

    private UserProfileResponseDTO.StatisticsResponse buildStatistics(Long userId) {
        UserStatistics stats = userStatisticsRepository.findByUserId(userId).orElse(null);

        return UserProfileResponseDTO.StatisticsResponse.builder()
                .practiceSessionCount(stats == null ? 0L : stats.getTotalPracticeCount().longValue())
                .totalPracticeMinutes(stats == null ? 0L : stats.getTotalPracticeMinutes().longValue())
                .completedLearningCount(countCompletedLearnings(userId))
                .build();
    }

    private Long countCompletedLearnings(Long userId) {
        return userLearningProgressRepository.countDistinctCompletedLearningsByUserId(userId);
    }
}
