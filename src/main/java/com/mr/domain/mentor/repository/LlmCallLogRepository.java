package com.mr.domain.mentor.repository;

import com.mr.domain.mentor.entity.LlmCallLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LlmCallLogRepository extends JpaRepository<LlmCallLog, Long> {
}
