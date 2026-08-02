package com.mr.domain.mentor.repository;

import com.mr.domain.mentor.entity.LlmCallLog;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LlmCallLogRepository extends JpaRepository<LlmCallLog, Long> {

    @Modifying(clearAutomatically = true)
    @Query("delete from LlmCallLog l where l.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
