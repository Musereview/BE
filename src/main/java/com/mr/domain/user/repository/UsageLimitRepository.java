package com.mr.domain.user.repository;

import com.mr.domain.user.entity.UsageLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsageLimitRepository extends JpaRepository<UsageLimit, Long> {

    @Modifying(clearAutomatically = true)
    @Query("delete from UsageLimit ul where ul.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
