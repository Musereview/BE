package com.mr.domain.statistics.repository;

import com.mr.domain.statistics.entity.UserStatistics;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStatisticsRepository extends JpaRepository<UserStatistics, Long> {

    Optional<UserStatistics> findByUser_UserId(Long userId);
}
