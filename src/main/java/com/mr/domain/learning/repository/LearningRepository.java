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

    List<Learning> findByCategoryAndIsActiveTrueOrderByTitleAsc(LearningCategory category);

    // 난이도별 대표 패키지 1개 (title 오름차순 기준 첫 번째)
    Optional<Learning> findFirstByCategoryAndDifficultyAndIsActiveTrueOrderByTitleAsc(
            LearningCategory category, LearningDifficulty difficulty);

    // 대표 패키지 3개 (title 오름차순)
    List<Learning> findTop3ByCategoryAndIsActiveTrueOrderByTitleAsc(LearningCategory category);
}
