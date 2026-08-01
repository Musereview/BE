package com.mr.domain.weakness.repository;

import com.mr.domain.weakness.entity.WeaknessNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeaknessNoteRepository extends JpaRepository<WeaknessNote, Long> {

    @Modifying
    @Query("delete from WeaknessNote w where w.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
