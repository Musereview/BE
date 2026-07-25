package com.mr.domain.learning.repository;

import com.mr.domain.learning.entity.Learning;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningRepository extends JpaRepository<Learning, Long> {
}
