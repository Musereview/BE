package com.mr.domain.statistics.repository;

import com.mr.domain.statistics.entity.SkillStatistics;
import com.mr.domain.statistics.entity.enums.PeriodType;
import com.mr.domain.statistics.entity.enums.SkillType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SkillStatisticsRepository extends JpaRepository<SkillStatistics, Long> {

    Optional<SkillStatistics> findByUser_UserIdAndPeriodTypeAndPeriodStartAndSkillType(
            Long userId, PeriodType periodType, LocalDate periodStart, SkillType skillType);

    @Modifying(clearAutomatically = true)
    @Query("delete from SkillStatistics ss where ss.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
