package com.mr.domain.learning.repository;

import com.mr.domain.learning.entity.ChordExample;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChordExampleRepository extends JpaRepository<ChordExample, Long> {
    List<ChordExample> findByLearningStep_Id(Long learningStepId);
}
