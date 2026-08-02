package com.mr.domain.learning.repository;

import com.mr.domain.learning.entity.UserLearningProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;

public interface UserLearningProgressRepository extends JpaRepository<UserLearningProgress, Long> {

    Optional<UserLearningProgress> findByUser_UserIdAndLearningStep_Id(Long userId, Long learningStepId);
    // 유저가 완료한(점수 90점 이상) 학습 단계 수 조회
    @Query("SELECT COUNT(ulp) FROM UserLearningProgress ulp " +
            "WHERE ulp.user.userId = :userId AND ulp.learning.id = :learningId AND ulp.score >= 90")
    long countCompletedStepsByUserIdAndLearningId(@Param("userId") Long userId, @Param("learningId") Long learningId);

    // 특정 학습의 단계별 진행 기록 전체 조회 (커리큘럼 조회에서 단계별 status/score 매핑용)
    List<UserLearningProgress> findByUser_UserIdAndLearning_Id(Long userId, Long learningId);

    // 여러 학습의 단계별 진행 기록 전체 조회 (추천 학습 계산 시 N+1 방지용)
    List<UserLearningProgress> findByUser_UserIdAndLearning_IdIn(Long userId, List<Long> learningIds);

    // 유저가 가장 최근에 학습한 진행 기록 1건 (학습 홈의 "최근 학습 이어서 하기"용).
    // 패키지가 비활성화(is_active=false)됐으면 건너뛰고 그다음으로 최근인 활성 패키지를 찾음(동시각 충돌 방지용 id 2차 정렬)
    Optional<UserLearningProgress> findFirstByUser_UserIdAndLearning_IsActiveTrueOrderByLastStudiedAtDescIdDesc(Long userId);

    // 여러 학습에 대한 완료(score>=90) 단계 수를 한 번에 집계 (목록 조회 시 N+1 방지용)
    @Query("SELECT ulp.learning.id as learningId, COUNT(ulp) as completedStepCount FROM UserLearningProgress ulp " +
            "WHERE ulp.user.userId = :userId AND ulp.learning.id IN :learningIds AND ulp.score >= 90 " +
            "GROUP BY ulp.learning.id")
    List<CompletedStepCount> countCompletedStepsByUserIdAndLearningIdIn(
            @Param("userId") Long userId, @Param("learningIds") List<Long> learningIds);

    interface CompletedStepCount {
        Long getLearningId();
        Long getCompletedStepCount();
    }

    // 유저가 완료(점수 90점 이상)한 학습(learning) distinct 개수 조회 (프로필 통계의 completedLearningCount)
    @Query("SELECT COUNT(DISTINCT ulp.learning.id) FROM UserLearningProgress ulp " +
            "WHERE ulp.user.userId = :userId AND ulp.score >= 90")
    long countDistinctCompletedLearningsByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("delete from UserLearningProgress ulp where ulp.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
