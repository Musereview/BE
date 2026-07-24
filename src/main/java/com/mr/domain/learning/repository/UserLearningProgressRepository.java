package com.mr.domain.learning.repository;

import com.mr.domain.learning.entity.UserLearningProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserLearningProgressRepository extends JpaRepository<UserLearningProgress, Long> {

    Optional<UserLearningProgress> findByUser_UserIdAndLearningId(Long userId, Long learningId);
    // 유저가 완료한(점수 90점 이상) 학습 단계 수 조회
    @Query("SELECT COUNT(ulp) FROM UserLearningProgress ulp " +
            "JOIN ulp.learningStep ls " +
            "WHERE ulp.user.userId = :userId AND ls.learning.id = :learningId AND ulp.score >= 90")
    long countCompletedStepsByUserIdAndLearningId(@Param("userId") Long userId, @Param("learningId") Long learningId);
}
