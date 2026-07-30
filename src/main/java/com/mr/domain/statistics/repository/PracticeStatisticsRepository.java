package com.mr.domain.statistics.repository;

import com.mr.domain.statistics.entity.PracticeStatistics;
import com.mr.domain.statistics.entity.enums.PeriodType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticeStatisticsRepository extends JpaRepository<PracticeStatistics, Long> {

    Optional<PracticeStatistics> findByUser_UserIdAndPeriodTypeAndPeriodStart(
            Long userId, PeriodType periodType, LocalDate periodStart);
}
