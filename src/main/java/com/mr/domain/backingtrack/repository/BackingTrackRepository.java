package com.mr.domain.backingtrack.repository;

import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.backingtrack.entity.enums.AccessLevel;
import com.mr.domain.backingtrack.entity.enums.ScaleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BackingTrackRepository extends JpaRepository<BackingTrack, Long> {

    Optional<BackingTrack> findByIdAndDeletedAtIsNull(Long id);
    
    // 최근 1주일 내에 분석이 완료된 공개 트랙 필터링
    @Query("SELECT DISTINCT b FROM BackingTrack b " +
            "JOIN Playing p ON p.backingTrack = b " +
            "JOIN Analysis a ON a.playing = p " +
            "WHERE b.accessLevel = :accessLevel " +
            "AND a.status = 'COMPLETED' " +
            "AND a.completedAt >= :oneWeekAgo " +
            "ORDER BY b.playCount DESC, b.createdAt DESC")
    List<BackingTrack> findTopByAccessLevelAndAnalysisCompleted(
            @Param("accessLevel") AccessLevel accessLevel,
            @Param("oneWeekAgo") LocalDateTime oneWeekAgo,
            Pageable pageable
    );

    // 서비스 단에서 깔끔하게 호출하기 위한 default 메서드 래핑
    default List<BackingTrack> findTop3RecommendedTracks(LocalDateTime oneWeekAgo) {
        return findTopByAccessLevelAndAnalysisCompleted(
                AccessLevel.PUBLIC,
                oneWeekAgo,
                org.springframework.data.domain.PageRequest.of(0, 3)
        );
    }

    // 장르, 키, 스케일, BPM 범위까지 모두 지원하는 동적 필터링 쿼리
    @Query("""
            SELECT b FROM BackingTrack b 
            WHERE b.accessLevel = :accessLevel 
              AND b.deletedAt IS NULL 
              AND (:genre IS NULL OR b.genre = :genre) 
              AND (:keySignature IS NULL OR b.keySignature = :keySignature)
              AND (:scaleType IS NULL OR b.scaleType = :scaleType)
              AND (:bpmMin IS NULL OR b.bpm >= :bpmMin)
              AND (:bpmMax IS NULL OR b.bpm <= :bpmMax)
            """)
    Page<BackingTrack> findFilteredTracks(
            @Param("accessLevel") AccessLevel accessLevel,
            @Param("genre") String genre,
            @Param("keySignature") String keySignature,
            @Param("scaleType") ScaleType scaleType,
            @Param("bpmMin") Integer bpmMin,
            @Param("bpmMax") Integer bpmMax,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true)
    @Query("UPDATE BackingTrack b SET b.playCount = b.playCount + 1 " +
            "WHERE b.id = :id AND b.deletedAt IS NULL")
    int increasePlayCount(@Param("id") Long id);

}
