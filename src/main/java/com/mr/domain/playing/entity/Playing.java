package com.mr.domain.playing.entity;

import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.playing.entity.enums.PlayingMode;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import com.mr.domain.playing.exception.MidiEventErrorStatus;
import com.mr.domain.playing.exception.PlayingErrorStatus;
import com.mr.domain.user.entity.User;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.entity.BaseCreatedDeletedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.mr.domain.playing.constant.MidiEventConstants.MAX_MIDI_EVENT_COUNT;

@Entity
@Table(
        name = "playing",
        indexes = {
                @Index(name = "idx_playing_user_status_ended_at",
                        columnList = "user_id, status, ended_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Playing extends BaseCreatedDeletedEntity {

    private static final int DEFAULT_BPM = 120;
    private static final int MIN_BPM = 50;
    private static final int MAX_BPM = 200;
    private static final int MAX_DURATION_SEC = 600;
    private static final long MAX_DURATION_MS = MAX_DURATION_SEC * 1000L;

    private static final long MIDI_TIMESTAMP_TOLERANCE_MS = 500L;       // 실제 연주 시간과 MIDI 이벤트 수집 종료 시점 간 허용 오차

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "playing_id")
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backing_track_id")
    private BackingTrack backingTrack;

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

    @Column(name = "recording_object_key", length = 255)
    private String recordingObjectKey;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "midi_data", columnDefinition = "jsonb", nullable = false)
    private List<MidiEventData> midiData = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private Playing(
            User user,
            BackingTrack backingTrack,
            PlayingMode mode,
            PlayingStatus status,
            Integer bpm,
            boolean isPublic
    ) {

        validateUser(user);
        validateMode(mode);
        validateStatus(status);
        validateBackingTrack(mode, backingTrack);

        this.user = user;
        this.backingTrack = backingTrack;
        this.mode = mode;
        this.status = status;
        this.bpm = resolveBpm(bpm);
        this.isPublic = isPublic;
        this.midiData = new ArrayList<>();
    }

    private static void validateUser(User user) {
        if (user == null) {
            throw new GeneralException(PlayingErrorStatus.MISSING_USER_ID);
        }
    }

    private static void validateBackingTrack(
            PlayingMode mode, BackingTrack backingTrack) {

        // 자유 연주 미지원 (BACKING_TRACK이 포함된 연주만 가능)
        if (mode != PlayingMode.BACKING_TRACK) {
            throw new GeneralException(PlayingErrorStatus.UNSUPPORTED_PLAYING_MODE);
        }

        if (backingTrack == null) {
            throw new GeneralException(PlayingErrorStatus.MISSING_BACKING_TRACK_ID);
        }
    }

    private static void validateMode(PlayingMode mode) {
        if (mode == null) {
            throw new GeneralException(PlayingErrorStatus.MISSING_PLAYING_MODE);
        }
    }

    private static void validateStatus(PlayingStatus status) {
        if (status == null) {
            throw new GeneralException(PlayingErrorStatus.MISSING_PLAYING_STATUS);
        }
    }

    private static int resolveBpm(Integer bpm) {
        int resolvedBpm = bpm == null ? DEFAULT_BPM : bpm;

        if (resolvedBpm < MIN_BPM || resolvedBpm > MAX_BPM) {
            throw new GeneralException(PlayingErrorStatus.INVALID_BPM_RANGE);
        }

        return resolvedBpm;
    }

    // 백킹트랙 연주 생성
    public static Playing createBackingTrack(
            User user, BackingTrack backingTrack, Integer bpm
    ) {
        return Playing.builder()
                .user(user)
                .backingTrack(backingTrack)
                .mode(PlayingMode.BACKING_TRACK)
                .status(PlayingStatus.READY)
                .bpm(bpm)
                .isPublic(false)
                .build();
    }

    // 연주 완료 시 전체 MIDI 데이터를 저장하고 완료 상태로 전환
    public void completeWithMidiData(
            List<MidiEventData> requestedMidiData,
            String recordingObjectKey
    ) {
        validateCompletableStatus();
        validateMidiData(requestedMidiData);

        LocalDateTime completedAt = LocalDateTime.now();
        long savedDurationMs = calculateSavedDurationMs(completedAt);

        List<MidiEventData> normalizedMidiData = normalizeMidiData(requestedMidiData, savedDurationMs);
        validateNormalizedMidiData(normalizedMidiData);
        validateRecordingObjectKey(recordingObjectKey);

        this.midiData = new ArrayList<>(normalizedMidiData);
        this.endedAt = this.startedAt.plusNanos(savedDurationMs * 1_000_000L);
        this.durationSec = Math.toIntExact(
                Duration.ofMillis(savedDurationMs).toSeconds());
        this.recordingObjectKey = recordingObjectKey;
        this.status = PlayingStatus.COMPLETED;
    }

    public void validatePlayingOwner(Long userId) {
        if (userId == null || this.user == null || !Objects.equals(this.user.getUserId(), userId)) {
            throw new GeneralException(PlayingErrorStatus.PLAYING_ACCESS_DENIED);
        }
    }

    private void validateRecordingObjectKey(String recordingObjectKey) {
        if (recordingObjectKey == null || recordingObjectKey.isBlank()) {
            throw new GeneralException(
                    PlayingErrorStatus.INVALID_RECORDING_OBJECT_KEY
            );
        }
    }

    public void validateCompleted() {
        if (this.status != PlayingStatus.COMPLETED) {
            throw new GeneralException(PlayingErrorStatus.PLAYING_NOT_COMPLETED);
        }
    }

    public void validateInProgress() {
        if (this.status != PlayingStatus.IN_PROGRESS) {
            throw new GeneralException(PlayingErrorStatus.INVALID_PLAYING_STATUS);
        }
    }

    public void start() {
        validateStartableStatus();

        this.status = PlayingStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    private void validateCompletableStatus() {
        if (this.status != PlayingStatus.IN_PROGRESS) {
            throw new GeneralException(MidiEventErrorStatus.PLAYING_NOT_IN_PROGRESS);
        }

        if (startedAt == null) {
            throw new GeneralException(PlayingErrorStatus.MISSING_PLAYING_START_TIME);
        }
    }

    private static void validateMidiData(List<MidiEventData> midiData) {
        if (midiData == null || midiData.isEmpty()) {
            throw new GeneralException(MidiEventErrorStatus.EMPTY_MIDI_EVENTS);
        }

        if (midiData.size() > MAX_MIDI_EVENT_COUNT) {
            throw new GeneralException(MidiEventErrorStatus.EXCEEDED_MIDI_EVENT_COUNT);
        }

        if (midiData.stream().anyMatch(Objects::isNull)) {
            throw new GeneralException(MidiEventErrorStatus.INVALID_MIDI_EVENT);
        }

        if (midiData.stream().anyMatch(Playing::hasInvalidEventOrder)) {
            throw new GeneralException(MidiEventErrorStatus.INVALID_MIDI_EVENT);
        }

        Set<MidiEventOrder> seenOrders = new HashSet<>();

        for (MidiEventData event : midiData) {
            MidiEventOrder order = new MidiEventOrder(
                    event.getTimestampMs(),
                    event.getSequence()
            );

            if (!seenOrders.add(order)) {
                throw new GeneralException(
                        MidiEventErrorStatus.DUPLICATE_MIDI_SEQUENCE
                );
            }
        }
    }

    private static void validateNormalizedMidiData(
            List<MidiEventData> normalizedMidiData
    ) {
        if (normalizedMidiData.isEmpty()) {
            throw new GeneralException(
                    MidiEventErrorStatus.EMPTY_MIDI_EVENTS
            );
        }
    }

    private record MidiEventOrder(
            Long timestampMs,
            Integer sequence
    ) {
    }

    private long calculateBackingTrackDurationMs() {
        if (backingTrack == null
                || backingTrack.getPlaytimeSec() == null
                || backingTrack.getPlaytimeSec() <= 0) {
            throw new GeneralException(
                    PlayingErrorStatus.INVALID_PLAYING_DURATION
            );
        }

        return backingTrack.getPlaytimeSec() * 1000L;
    }

    private long calculateSavedDurationMs(
            LocalDateTime completedAt
    ) {
        long actualDurationMs =
                Duration.between(
                        this.startedAt,
                        completedAt
                ).toMillis();

        if (actualDurationMs < 0) {
            throw new GeneralException(
                    PlayingErrorStatus.INVALID_PLAYING_DURATION
            );
        }

        long backingTrackDurationMs =
                calculateBackingTrackDurationMs();

        return Math.min(
                Math.min(actualDurationMs, backingTrackDurationMs),
                MAX_DURATION_MS
        );
    }

    public List<MidiEventData> getMidiData() {

        if (this.midiData == null) {
            return List.of();
        }
        return List.copyOf(this.midiData);
    }

    private void validateStartableStatus() {
        if (status != PlayingStatus.READY) {
            throw new GeneralException(PlayingErrorStatus.INVALID_PLAYING_STATUS);
        }
    }

    private static List<MidiEventData> normalizeMidiData(
            List<MidiEventData> midiData, long backingTrackDurationMs) {
        long allowedTimestampMs =
                Math.min(backingTrackDurationMs + MIDI_TIMESTAMP_TOLERANCE_MS, MAX_DURATION_MS);

        return midiData.stream()
                .filter(event -> event.getTimestampMs() <= allowedTimestampMs)
                .sorted(Comparator.comparingLong(MidiEventData::getTimestampMs)
                                .thenComparingInt(MidiEventData::getSequence)
                ).toList();
    }

    private static boolean hasInvalidEventOrder(
            MidiEventData event
    ) {
        return event.getTimestampMs() == null
                || event.getTimestampMs() < 0
                || event.getSequence() == null
                || event.getSequence() < 0;
    }
}
