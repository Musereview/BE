package com.mr.domain.learning.repository;

import com.mr.domain.learning.entity.UserLearningProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserLearningProgressRepository extends JpaRepository<UserLearningProgress, Long> {

    Optional<UserLearningProgress> findByUser_UserIdAndLearningStep_Id(Long userId, Long learningStepId);
    // 유저가 완료한(점수 90점 이상) 학습 단계 수 조회
    @Query("SELECT COUNT(ulp) FROM UserLearningProgress ulp " +
            "WHERE ulp.user.userId = :userId AND ulp.learning.id = :learningId AND ulp.score >= 90")
    long countCompletedStepsByUserIdAndLearningId(@Param("userId") Long userId, @Param("learningId") Long learningId);

    // 유저가 완료(점수 90점 이상)한 학습(learning) distinct 개수 조회 (프로필 통계의 completedLearningCount)
    @Query("SELECT COUNT(DISTINCT ulp.learning.id) FROM UserLearningProgress ulp " +
            "WHERE ulp.user.userId = :userId AND ulp.score >= 90")
    long countDistinctCompletedLearningsByUserId(@Param("userId") Long userId);
}
