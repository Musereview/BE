package com.mr.domain.learning.service;

import com.mr.domain.learning.dto.req.LearningResultSaveRequestDTO;
import com.mr.domain.learning.dto.res.LearningResultResponseDTO;
import com.mr.domain.learning.entity.Learning;
import com.mr.domain.learning.entity.UserLearningProgress;
import com.mr.domain.learning.exception.LearningErrorStatus;
import com.mr.domain.learning.repository.LearningRepository;
import com.mr.domain.learning.repository.UserLearningProgressRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LearningService {

    private final UserLearningProgressRepository userLearningProgressRepository;
    private final LearningRepository learningRepository;
    // 임시 작명
    private final UserRepository userRepository;

    public LearningResultResponseDTO.SaveResultResultDTO saveResult(
            Long userId,
            Long learningId,
            LearningResultSaveRequestDTO.SaveResultDTO request
    ){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        Learning learning = learningRepository.findById(learningId)
                .orElseThrow(() -> new GeneralException(LearningErrorStatus.LEARNING_NOT_FOUND));

        UserLearningProgress progress = userLearningProgressRepository
                .findByUserIdAndLearningId(userId, learning.getId())
                .map(p -> {
                    p.updateProgress(request.score(), LocalDateTime.now());
                    return p;
                })
                .orElseGet(() -> {
                    UserLearningProgress newProgress = UserLearningProgress.create(user, learning, null);
                    newProgress.updateProgress(request.score(), LocalDateTime.now());
                    return userLearningProgressRepository.save(newProgress);
                });
        return LearningResultResponseDTO.SaveResultResultDTO.from(progress);
    }
}
