package com.mr.domain.learning.repository;

import com.mr.domain.learning.entity.Learning;
import com.mr.domain.learning.entity.enums.LearningCategory;
import com.mr.domain.learning.entity.enums.LearningDifficulty;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LearningRepository extends JpaRepository<Learning, Long> {
    Optional<Learning> findByIdAndIsActiveTrue(Long id);

    // 같은 패키지(learningId)에 대한 학습 결과 저장을 트랜잭션 단위로 직렬화하기 위한 비관적 락 조회.
    // saveResult()에서 완료 개수를 "읽고-쓰고-다시 읽는" 구간 전체를 이 락으로 감싸서,
    // 동시에 다른 단계가 저장되는 경우의 완료 알림 누락/중복(TOCTOU)을 막는다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Learning l where l.id = :id")
    Optional<Learning> findByIdForUpdate(@Param("id") Long id);

    List<Learning> findByCategoryAndDifficultyAndIsActiveTrueOrderByTitleAsc(
            LearningCategory category, LearningDifficulty difficulty);

    List<Learning> findByCategoryAndIsActiveTrueOrderByTitleAsc(LearningCategory category);

    // 난이도별 대표 패키지 1개 (title 오름차순 기준 첫 번째)
    Optional<Learning> findFirstByCategoryAndDifficultyAndIsActiveTrueOrderByTitleAsc(
            LearningCategory category, LearningDifficulty difficulty);

    // 대표 패키지 3개 (title 오름차순)
    List<Learning> findTop3ByCategoryAndIsActiveTrueOrderByTitleAsc(LearningCategory category);
}
