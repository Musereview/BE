package com.mr.domain.learning.repository;

import com.mr.domain.learning.entity.Learning;
import com.mr.domain.learning.entity.enums.LearningCategory;
import com.mr.domain.learning.entity.enums.LearningDifficulty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearningRepository extends JpaRepository<Learning, Long> {
    Optional<Learning> findByIdAndIsActiveTrue(Long id);

    List<Learning> findByCategoryAndDifficultyAndIsActiveTrueOrderByTitleAsc(
            LearningCategory category, LearningDifficulty difficulty);
}
