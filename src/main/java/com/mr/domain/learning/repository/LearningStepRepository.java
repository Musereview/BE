package com.mr.domain.learning.repository;

import com.mr.domain.learning.entity.LearningStep;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningStepRepository extends JpaRepository<LearningStep, Long> {
    // 특정 학습에 속한 전체 단계 수 조회
    long countByLearningId(Long learningId);
}
