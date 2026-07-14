package com.mr.domain.playing.entity;

import com.mr.domain.playing.entity.enums.PlayingMode;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import com.mr.domain.playing.exception.PlayingErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.entity.BaseCreatedDeletedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "playing")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Playing extends BaseCreatedDeletedEntity {

    private static final int DEFAULT_BPM = 120;
    private static final int MIN_BPM = 50;
    private static final int MAX_BPM = 200;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "midi_data", columnDefinition = "jsonb", nullable = false)
    private List<MidiEventData> midiData = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private Playing(
            Long userId,
            Long backingTrackId,
            PlayingMode mode,
            PlayingStatus status,
            Integer bpm,
            boolean isPublic
    ) {

        validateUserId(userId);
        validateBackingTrack(mode, backingTrackId);

        this.userId = userId;
        this.backingTrackId = backingTrackId;
        this.mode = mode;
        this.status = status;
        this.bpm = resolveBpm(bpm);
        this.isPublic = isPublic;
        this.midiData = new ArrayList<>();
    }

    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw new GeneralException(PlayingErrorStatus.MISSING_USER_ID);
        }
    }

    private static void validateBackingTrack(
            PlayingMode mode, Long backingTrackId) {
        if (mode == PlayingMode.BACKING_TRACK && backingTrackId == null) {
            throw new GeneralException(PlayingErrorStatus.MISSING_BACKING_TRACK_ID);
        }
    }

    private static int resolveBpm(Integer bpm) {
        int resolvedBpm = bpm == null ? DEFAULT_BPM : bpm;

        if (resolvedBpm < MIN_BPM || resolvedBpm > MAX_BPM) {
            throw new GeneralException(PlayingErrorStatus.INVALID_BPM_RANGE);
        }

        return resolvedBpm;
    }

    // 자유 연주 생성
    public static Playing createFreePlay(
            Long userId, Integer bpm
    ) {
        return Playing.builder()
                .userId(userId)
                .mode(PlayingMode.FREE_PLAY)
                .status(PlayingStatus.READY)
                .bpm(bpm)
                .isPublic(false)
                .build();
    }

    // 백킹트랙 연주 생성
    public static Playing createBackingTrack(
            Long userId, Long backingTrackId, Integer bpm
    ) {
        return Playing.builder()
                .userId(userId)
                .backingTrackId(backingTrackId)
                .mode(PlayingMode.BACKING_TRACK)
                .status(PlayingStatus.READY)
                .bpm(bpm)
                .isPublic(false)
                .build();
    }

    public void saveMidiData(List<MidiEventData> midiData) {
        validateMidiData(midiData);

        this.midiData = new ArrayList<>(midiData);
        this.midiData.sort(Comparator.comparingLong(MidiEventData::getTimestampMs));

    }

    private static void validateMidiData(List<MidiEventData> midiData) {
        if (midiData == null || midiData.isEmpty()) {
            throw new GeneralException(PlayingErrorStatus.EMPTY_MIDI_EVENTS);
        }

        if (midiData.stream().anyMatch(Objects::isNull)) {
            throw new GeneralException(PlayingErrorStatus.INVALID_MIDI_EVENT);
        }
    }

    public List<MidiEventData> getMidiData() {

        if (this.midiData == null) {
            return List.of();
        }
        return List.copyOf(this.midiData);
    }
}
