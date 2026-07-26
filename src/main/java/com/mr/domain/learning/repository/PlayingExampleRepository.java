package com.mr.domain.learning.repository;

import com.mr.domain.learning.entity.PlayingExample;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayingExampleRepository extends JpaRepository<PlayingExample, Long> {
    Optional<PlayingExample> findByLearningStep_Id(Long learningStepId);
}
