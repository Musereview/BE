package com.mr.domain.user.service;

import com.mr.domain.subscriptions.entity.Subscription;
import com.mr.domain.subscriptions.repository.SubscriptionRepository;
import com.mr.domain.statistics.entity.UserStatistics;
import com.mr.domain.statistics.repository.UserStatisticsRepository;
import com.mr.domain.user.dto.UserProfileRequestDTO;
import com.mr.domain.user.dto.UserProfileResponseDTO;
import com.mr.domain.user.entity.Instrument;
import com.mr.domain.user.entity.Student;
import com.mr.domain.user.entity.StudentInstrument;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.exception.StudentErrorStatus;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.InstrumentRepository;
import com.mr.domain.user.repository.StudentInstrumentRepository;
import com.mr.domain.user.repository.StudentRepository;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.security.SecurityUtil;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileService {

    private static final String PIANO_CODE = "PIANO";
    private static final int SUBSCRIPTION_PERIOD_DAYS = 30;

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StudentInstrumentRepository studentInstrumentRepository;
    private final InstrumentRepository instrumentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserStatisticsRepository userStatisticsRepository;

    public UserProfileResponseDTO.ProfileResponse getMyProfile() {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = getUser(userId);
        Student student = getStudent(user);

        String instrumentType = studentInstrumentRepository.findByStudentAndPrimaryTrue(student)
                .map(StudentInstrument::getInstrument)
                .map(Instrument::getCode)
                .orElseThrow(() -> new IllegalStateException("대표 악기 정보가 존재하지 않습니다."));

        String subscriptionTier = subscriptionRepository.findFirstByUserOrderByStartDateDesc(user)
                .map(Subscription::getTier)
                .orElseThrow(() -> new IllegalStateException("구독 정보가 존재하지 않습니다."));

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

        user.updateNickname(request.nickname());
        ensureNicknameNotTaken(user);

        Student student = studentRepository.save(Student.create(user, request.skillLevel()));

        Instrument piano = instrumentRepository.findByCode(PIANO_CODE)
                .orElseThrow(() -> new IllegalStateException("PIANO 악기 시드 데이터가 존재하지 않습니다."));
        studentInstrumentRepository.save(StudentInstrument.createPrimary(student, piano));

        String tier = request.subscriptionTier() == null ? null : request.subscriptionTier().name();
        LocalDateTime now = LocalDateTime.now();
        Subscription subscription = subscriptionRepository.save(
                Subscription.create(user, tier, now, now.plusDays(SUBSCRIPTION_PERIOD_DAYS)));

        return UserProfileResponseDTO.OnboardingResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .skillLevel(student.getTheoryLevel())
                .instrumentType(piano.getCode())
                .subscriptionTier(subscription.getTier())
                .profileImgUrl(user.getProfileImgUrl())
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

    private void ensureNicknameNotTaken(User user) {
        if (userRepository.existsByNicknameAndUserIdNot(user.getNickname(), user.getUserId())) {
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

    // TODO: feat/#32-learning-status-api(동균 강) 병합 후 UserLearningProgressRepository로 교체
    // SELECT COUNT(DISTINCT ulp.learning_id) FROM user_learning_progress ulp
    // WHERE ulp.user_id = :userId AND ulp.score >= 90
    private Long countCompletedLearnings(Long userId) {
        return 0L;
    }
}
