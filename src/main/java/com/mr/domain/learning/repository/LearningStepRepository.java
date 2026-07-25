package com.mr.domain.learning.repository;

import com.mr.domain.learning.entity.LearningStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearningStepRepository extends JpaRepository<LearningStep, Long> {
    // 특정 학습에 속한 전체 단계 수 조회
    long countByLearningId(Long learningId);

    // 특정 학습에 속한 단계 목록, step_no 오름차순
    List<LearningStep> findByLearning_IdOrderByStepNoAsc(Long learningId);
}
