package com.mr.domain.learning.service;

import com.mr.domain.learning.dto.req.LearningResultSaveRequestDTO;
import com.mr.domain.learning.dto.res.LearningProgressResponseDTO;
import com.mr.domain.learning.dto.res.LearningResultResponseDTO;
import com.mr.domain.learning.entity.Learning;
import com.mr.domain.learning.entity.LearningStep;
import com.mr.domain.learning.entity.UserLearningProgress;
import com.mr.domain.learning.exception.LearningErrorStatus;
import com.mr.domain.learning.repository.LearningRepository;
import com.mr.domain.learning.repository.LearningStepRepository;
import com.mr.domain.learning.repository.UserLearningProgressRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LearningService {

    private final UserLearningProgressRepository userLearningProgressRepository;
    private final LearningStepRepository learningStepRepository;
    private final LearningRepository learningRepository;
    // 임시 작명
    private final UserRepository userRepository;

    // 학습 결과 저장
    @Transactional
    public LearningResultResponseDTO.SaveResultResultDTO saveResult(
            Long learningId,
            LearningResultSaveRequestDTO.SaveResultDTO request
    ){
        Long userId = SecurityUtil.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        Learning learning = learningRepository.findById(learningId)
                .orElseThrow(() -> new GeneralException(LearningErrorStatus.LEARNING_NOT_FOUND));

        LearningStep learningStep = learningStepRepository.findById(request.learningStepId())
                .orElseThrow(() -> new GeneralException(LearningErrorStatus.LEARNING_STEP_NOT_FOUND));

        UserLearningProgress progress = userLearningProgressRepository
                .findByUser_UserIdAndLearningId(userId, learning.getId())
                .map(p -> {
                    p.updateProgress(request.score(), LocalDateTime.now());
                    return p;
                })
                .orElseGet(() -> {
                    UserLearningProgress newProgress = UserLearningProgress.create(user, learning, learningStep);
                    newProgress.updateProgress(request.score(), LocalDateTime.now());
                    return userLearningProgressRepository.save(newProgress);
                });
        return LearningResultResponseDTO.SaveResultResultDTO.from(progress);
    }

    // 학습 진행률 조회 로직
    public LearningProgressResponseDTO.ProgressResultDTO getLearningProgress(Long learningId) {
        // 학습 존재 여부 확인
        if (!learningRepository.existsById(learningId)) {
            throw new GeneralException(LearningErrorStatus.LEARNING_NOT_FOUND);
        }

        // 현재 로그인한 유저 ID 획득 (SecurityUtil 활용)
        Long userId = SecurityUtil.getCurrentUserId();

        // 전체 학습 단계 수 조회
        long totalStepCount = learningStepRepository.countByLearningId(learningId);

        // 완료한 학습 단계 수 조회
        long completedStepCount = userLearningProgressRepository.countCompletedStepsByUserIdAndLearningId(userId, learningId);

        // 진행률 계산 (0으로 나누기 예외 방지)
        int progressRate = totalStepCount == 0 ? 0 : (int) Math.round((double) completedStepCount / totalStepCount * 100);

        return LearningProgressResponseDTO.ProgressResultDTO.of(learningId, progressRate);
    }
}
