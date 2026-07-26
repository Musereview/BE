package com.mr.domain.backingTrack.repository;

import com.mr.domain.backingTrack.entity.BackingTrack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BackingTrackRepository extends JpaRepository<BackingTrack, Long> {

    Optional<BackingTrack> findByIdAndDeletedAtIsNull(Long id);
}
