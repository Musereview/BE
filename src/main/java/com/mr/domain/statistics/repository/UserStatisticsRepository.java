package com.mr.domain.statistics.repository;

import com.mr.domain.statistics.entity.UserStatistics;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserStatisticsRepository extends JpaRepository<UserStatistics, Long> {

    Optional<UserStatistics> findByUser_UserId(Long userId);

    @Modifying
    @Query("delete from UserStatistics us where us.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
