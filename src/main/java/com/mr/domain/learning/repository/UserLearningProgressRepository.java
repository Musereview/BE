package com.mr.domain.learning.repository;

import com.mr.domain.learning.entity.UserLearningProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserLearningProgressRepository extends JpaRepository<UserLearningProgress, Long> {

    Optional<UserLearningProgress> findByUserIdAndLearningId(Long userId, Long learningId);
}
