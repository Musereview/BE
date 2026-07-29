package com.mr.domain.learning.repository;

import com.mr.domain.learning.entity.LearningStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LearningStepRepository extends JpaRepository<LearningStep, Long> {
    // 특정 학습에 속한 전체 단계 수 조회
    long countByLearningId(Long learningId);

    // 특정 학습에 속한 단계 목록, step_no 오름차순
    List<LearningStep> findByLearning_IdOrderByStepNoAsc(Long learningId);

    // 여러 학습의 전체 단계 수를 한 번에 집계 (목록 조회 시 N+1 방지용)
    @Query("SELECT ls.learning.id as learningId, COUNT(ls) as stepCount FROM LearningStep ls " +
            "WHERE ls.learning.id IN :learningIds GROUP BY ls.learning.id")
    List<LearningIdCount> countByLearningIdIn(@Param("learningIds") List<Long> learningIds);

    interface LearningIdCount {
        Long getLearningId();
        Long getStepCount();
    }
}
