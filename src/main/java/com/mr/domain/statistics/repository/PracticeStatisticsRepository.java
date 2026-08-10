package com.mr.domain.statistics.repository;

import com.mr.domain.statistics.entity.PracticeStatistics;
import com.mr.domain.statistics.entity.enums.PeriodType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PracticeStatisticsRepository extends JpaRepository<PracticeStatistics, Long> {

    Optional<PracticeStatistics> findByUser_UserIdAndPeriodTypeAndPeriodStart(
            Long userId, PeriodType periodType, LocalDate periodStart);

    List<PracticeStatistics> findAllByUser_UserIdAndPeriodTypeAndPeriodStartBetween(
            Long userId, PeriodType periodType, LocalDate from, LocalDate to);

    @Modifying(clearAutomatically = true)
    @Query("delete from PracticeStatistics ps where ps.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
