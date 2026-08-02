package com.mr.domain.backingtrack.repository;

import com.mr.domain.backingtrack.entity.ChordProgression;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChordProgressionRepository extends JpaRepository<ChordProgression, Long> {

    @Modifying(clearAutomatically = true)
    @Query("delete from ChordProgression cp where cp.backingTrack.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
