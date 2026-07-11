package com.mr.domain.playing.entity;

import com.mr.domain.playing.entity.enums.PlayingMode;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import com.mr.domain.playing.exception.PlayingErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.entity.BaseCreatedDeletedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table (name = "playing")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Playing extends BaseCreatedDeletedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "playing_id")
    private Long id;

    // TODO: 유저 ID 연관 관계 설정 예정
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // TODO: 백킹트랙 ID 연관 관계 설정 예정
    @Column(name = "backing_track_id")
    private Long backingTrackId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false)
    private PlayingMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PlayingStatus status;

    @Column(name = "bpm", nullable = false)
    private Integer bpm;

    @Column(name = "metronome_enabled", nullable = false)
    private boolean metronomeEnabled;

    // 실제 연주가 시작된 시간
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    // 사용자가 연주한 녹음 파일 URL
    @Column(name = "recording_file_url", length = 255)
    private String recordingFileUrl;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Builder(access = AccessLevel.PRIVATE)
    private Playing(
            Long userId,
            Long backingTrackId,
            PlayingMode mode,
            PlayingStatus status,
            Integer bpm,
            boolean metronomeEnabled,
            boolean isPublic
    ) {

        validateUserId(userId);
        validateBpm(bpm);

        this.userId = userId;
        this.backingTrackId = backingTrackId;
        this.mode = mode;
        this.status = status;
        this.bpm = bpm;
        this.metronomeEnabled = metronomeEnabled;
        this.isPublic = isPublic;
    }

    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw new GeneralException(PlayingErrorStatus.MISSING_USER_ID);
        }
    }

    private static void validateBpm(Integer bpm) {
        if (bpm != null && (bpm < 50 || bpm > 200)) {
            throw new GeneralException(PlayingErrorStatus.INVALID_BPM_RANGE);
        }
    }

    // 자유 연주 생성
    public static Playing createFreePlay(
            Long userId, Integer bpm, boolean metronomeEnabled
    ) {
        return Playing.builder()
                .userId(userId)
                .mode(PlayingMode.FREE_PLAY)
                .status(PlayingStatus.READY)
                .bpm( bpm != null ? bpm : 120)
                .metronomeEnabled(metronomeEnabled)
                .isPublic(false)
                .build();
    }

    // 백킹트랙 연주 생성
    public static Playing createBackingTrack(
            Long userId, Long backingTrackId,
            Integer bpm, boolean metronomeEnabled
    ) {

        if ( backingTrackId == null) {
            throw new GeneralException(PlayingErrorStatus.MISSING_BACKING_TRACK_ID);
        }

        return Playing.builder()
                .userId(userId)
                .backingTrackId(backingTrackId)
                .mode(PlayingMode.BACKING_TRACK)
                .status(PlayingStatus.READY)
                .bpm ( bpm != null ? bpm : 120 )
                .metronomeEnabled(metronomeEnabled)
                .isPublic(false)
                .build();
    }
}
