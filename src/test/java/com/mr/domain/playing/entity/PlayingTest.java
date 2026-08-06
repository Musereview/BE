package com.mr.domain.playing.entity;

import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.playing.entity.enums.MidiType;
import com.mr.domain.playing.entity.enums.PlayingMode;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import com.mr.domain.playing.exception.MidiEventErrorStatus;
import com.mr.domain.playing.exception.PlayingErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.mr.domain.playing.constant.MidiEventConstants.MAX_MIDI_EVENT_COUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlayingTest {

    private static final int BPM = 120;
    private static final String RECORDING_OBJECT_KEY =
            "recordings/1/2026-08-02/150000_a1b2c3.mp3";

    private static final String BUCKET = "test-bucket";
    private static final String REGION = "ap-northeast-2";

    private static final String OBJECT_KEY =
            "recordings/1/2026-08-02/033746_e90683.mp3";

    private static final String RECORDING_FILE_URL =
            "https://" + BUCKET + ".s3." + REGION + ".amazonaws.com/" + OBJECT_KEY;

    @Test
    @DisplayName("READY 상태의 연주를 시작하면 IN_PROGRESS 상태로 변경된다")
    void startPlaying() {
        // given
        Playing playing = createReadyPlaying();

        // when
        playing.start();

        // then
        assertThat(playing.getStatus())
                .isEqualTo(PlayingStatus.IN_PROGRESS);

        assertThat(playing.getStartedAt())
                .isNotNull();
    }

    @Test
    @DisplayName("진행 중인 연주에 MIDI 데이터를 저장하면 완료 상태로 변경된다")
    void completeWithMidiData() {
        // given
        Playing playing = createInProgressPlaying();

        List<MidiEventData> midiData = List.of(
                createMidiEvent(0, 100L),
                createMidiEvent(1, 200L),
                createMidiEvent(2, 300L)
        );

        // when
        playing.completeWithMidiData(midiData, RECORDING_OBJECT_KEY);

        // then
        assertThat(playing.getMidiData())
                .hasSize(3);

        assertThat(playing.getEndedAt())
                .isNotNull();

        assertThat(playing.getDurationSec())
                .isNotNull()
                .isGreaterThanOrEqualTo(0)
                .isLessThanOrEqualTo(600);

        assertThat(playing.getStatus())
                .isEqualTo(PlayingStatus.COMPLETED);
    }

    @Test
    @DisplayName("MIDI 이벤트는 timestampMs와 sequence 오름차순으로 저장된다")
    void sortMidiData() {
        // given
        Playing playing = createInProgressPlaying();

        List<MidiEventData> midiData = List.of(
                createMidiEvent(3, 300L),
                createMidiEvent(2, 100L),
                createMidiEvent(1, 100L),
                createMidiEvent(4, 200L)
        );

        // when
        playing.completeWithMidiData(midiData, RECORDING_OBJECT_KEY);

        // then
        assertThat(playing.getMidiData())
                .extracting(
                        MidiEventData::getTimestampMs,
                        MidiEventData::getSequence
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(100L, 1),
                        org.assertj.core.groups.Tuple.tuple(100L, 2),
                        org.assertj.core.groups.Tuple.tuple(200L, 4),
                        org.assertj.core.groups.Tuple.tuple(300L, 3)
                );
    }

    @Test
    @DisplayName("MIDI 이벤트 목록이 비어 있으면 EMPTY_MIDI_EVENTS 예외가 발생한다")
    void rejectEmptyMidiData() {
        Playing playing = createInProgressPlaying();

        assertThatThrownBy(() ->
                playing.completeWithMidiData(List.of(), RECORDING_OBJECT_KEY)
        )
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> {
                    GeneralException generalException =
                            (GeneralException) exception;

                    assertThat(generalException.getCode())
                            .isEqualTo(
                                    MidiEventErrorStatus.EMPTY_MIDI_EVENTS
                            );
                });
    }

    @Test
    @DisplayName("MIDI 이벤트 목록이 null이면 EMPTY_MIDI_EVENTS 예외가 발생한다")
    void rejectNullMidiData() {
        // given
        Playing playing = createInProgressPlaying();

        // when & then
        assertThatThrownBy(() ->
                playing.completeWithMidiData(null, RECORDING_OBJECT_KEY)
        )
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> {
                    GeneralException generalException =
                            (GeneralException) exception;

                    assertThat(generalException.getCode())
                            .isEqualTo(
                                    MidiEventErrorStatus.EMPTY_MIDI_EVENTS
                            );
                });
    }

    @Test
    @DisplayName("MIDI 이벤트 목록에 null 요소가 있으면 INVALID_MIDI_EVENT 예외가 발생한다")
    void rejectNullMidiEvent() {
        // given
        Playing playing = createInProgressPlaying();

        List<MidiEventData> midiData = new ArrayList<>();
        midiData.add(createMidiEvent(0, 100L));
        midiData.add(null);

        // when & then
        assertThatThrownBy(() ->
                playing.completeWithMidiData(midiData, RECORDING_OBJECT_KEY)
        )
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> {
                    GeneralException generalException =
                            (GeneralException) exception;

                    assertThat(generalException.getCode())
                            .isEqualTo(
                                    MidiEventErrorStatus.INVALID_MIDI_EVENT
                            );
                });
    }

    @Test
    @DisplayName("timestampMs와 sequence가 모두 같으면 DUPLICATE_MIDI_SEQUENCE 예외가 발생한다")
    void rejectDuplicateMidiEventOrder() {
        // given
        Playing playing = createInProgressPlaying();

        List<MidiEventData> midiData = List.of(
                createMidiEvent(1, 100L),
                createMidiEvent(1, 100L)
        );

        // when & then
        assertThatThrownBy(() ->
                playing.completeWithMidiData(midiData, RECORDING_OBJECT_KEY)
        )
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> {
                    GeneralException generalException =
                            (GeneralException) exception;

                    assertThat(generalException.getCode())
                            .isEqualTo(
                                    MidiEventErrorStatus.DUPLICATE_MIDI_SEQUENCE
                            );
                });
    }

    @Test
    @DisplayName("sequence가 같아도 timestampMs가 다르면 저장할 수 있다")
    void acceptSameSequenceWithDifferentTimestamp() {
        // given
        Playing playing = createInProgressPlaying();

        List<MidiEventData> midiData = List.of(
                createMidiEvent(1, 100L),
                createMidiEvent(1, 200L)
        );

        // when
        playing.completeWithMidiData(midiData, RECORDING_OBJECT_KEY);

        // then
        assertThat(playing.getMidiData())
                .hasSize(2);
    }

    @Test
    @DisplayName("MIDI 이벤트가 최대 개수이면 저장할 수 있다")
    void acceptMaximumMidiEventCount() {
        // given
        Playing playing = createInProgressPlaying();

        List<MidiEventData> midiData =
                new ArrayList<>(MAX_MIDI_EVENT_COUNT);

        for (int sequence = 0;
             sequence < MAX_MIDI_EVENT_COUNT;
             sequence++) {

            midiData.add(
                    createMidiEvent(sequence, 0L)
            );
        }

        // when
        playing.completeWithMidiData(midiData, RECORDING_OBJECT_KEY);

        // then
        assertThat(playing.getMidiData())
                .hasSize(MAX_MIDI_EVENT_COUNT);

        assertThat(playing.getStatus())
                .isEqualTo(PlayingStatus.COMPLETED);
    }

    @Test
    @DisplayName("MIDI 이벤트 최대 개수를 초과하면 EXCEEDED_MIDI_EVENT_COUNT 예외가 발생한다")
    void rejectExceededMidiEventCount() {
        // given
        Playing playing = createInProgressPlaying();

        List<MidiEventData> midiData =
                new ArrayList<>(MAX_MIDI_EVENT_COUNT + 1);

        for (int sequence = 0;
             sequence <= MAX_MIDI_EVENT_COUNT;
             sequence++) {

            midiData.add(
                    createMidiEvent(sequence, 0L)
            );
        }

        // when & then
        assertThatThrownBy(() ->
                playing.completeWithMidiData(midiData, RECORDING_OBJECT_KEY)
        )
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> {
                    GeneralException generalException =
                            (GeneralException) exception;

                    assertThat(generalException.getCode())
                            .isEqualTo(
                                    MidiEventErrorStatus.EXCEEDED_MIDI_EVENT_COUNT
                            );
                });
    }

    @Test
    @DisplayName("완료된 연주에는 MIDI 데이터를 다시 저장할 수 없다")
    void rejectCompletedPlaying() {
        // given
        Playing playing = createInProgressPlaying();

        playing.completeWithMidiData(
                List.of(createMidiEvent(0, 0L)),
                RECORDING_OBJECT_KEY
        );

        // when & then
        assertThatThrownBy(() ->
                playing.completeWithMidiData(
                        List.of(createMidiEvent(1, 100L)),
                        RECORDING_OBJECT_KEY
                )
        )
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> {
                    GeneralException generalException =
                            (GeneralException) exception;

                    assertThat(generalException.getCode())
                            .isEqualTo(
                                    MidiEventErrorStatus.PLAYING_NOT_IN_PROGRESS
                            );
                });
    }

    @Test
    @DisplayName("허용 시간을 초과한 MIDI 이벤트만 존재하면 완료 처리하지 않는다")
    void rejectMidiDataEmptyAfterNormalization() {
        // given
        Playing playing = createInProgressPlaying();

        List<MidiEventData> midiData =
                List.of(
                        createMidiEvent(
                                0,
                                600_001L
                        )
                );

        // when & then
        assertThatThrownBy(() ->
                playing.completeWithMidiData(midiData, RECORDING_OBJECT_KEY)
        )
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> {
                    GeneralException generalException =
                            (GeneralException) exception;

                    assertThat(generalException.getCode())
                            .isEqualTo(
                                    MidiEventErrorStatus.EMPTY_MIDI_EVENTS
                            );
                });

        assertThat(playing.getStatus())
                .isEqualTo(PlayingStatus.IN_PROGRESS);

        assertThat(playing.getMidiData())
                .isEmpty();

        assertThat(playing.getEndedAt())
                .isNull();

        assertThat(playing.getDurationSec())
                .isNull();
    }

    @Test
    @DisplayName("READY 상태에서는 PLAYING_NOT_IN_PROGRESS 예외가 발생한다")
    void rejectReadyPlaying() {
        // given
        Playing playing = createReadyPlaying();

        // when & then
        assertThatThrownBy(() ->
                playing.completeWithMidiData(
                        List.of(createMidiEvent(0, 0L)),
                        RECORDING_OBJECT_KEY
                )
        )
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> {
                    GeneralException generalException =
                            (GeneralException) exception;

                    assertThat(generalException.getCode())
                            .isEqualTo(
                                    MidiEventErrorStatus.PLAYING_NOT_IN_PROGRESS
                            );
                });
    }

    @Test
    @DisplayName("연주 생성 시 BPM이 null이면 기본값 120이 적용된다")
    void applyDefaultBpm() {
        // given
        User user = mock(User.class);
        BackingTrack backingTrack = mock(BackingTrack.class);

        // when
        Playing playing = Playing.createBackingTrack(
                user,
                backingTrack,
                null
        );

        // then
        assertThat(playing.getBpm())
                .isEqualTo(120);
    }

    @Test
    @DisplayName("연주 생성 시 BPM이 허용 범위를 벗어나면 INVALID_BPM_RANGE 예외가 발생한다")
    void rejectInvalidBpm() {
        // given
        User user = mock(User.class);
        BackingTrack backingTrack = mock(BackingTrack.class);

        // when & then
        assertThatThrownBy(() ->
                Playing.createBackingTrack(
                        user,
                        backingTrack,
                        201
                )
        )
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> {
                    GeneralException generalException =
                            (GeneralException) exception;

                    assertThat(generalException.getCode())
                            .isEqualTo(
                                    PlayingErrorStatus.INVALID_BPM_RANGE
                            );
                });
    }

    @Test
    @DisplayName("원본 MIDI 목록을 변경해도 엔티티에 저장된 목록은 변경되지 않는다")
    void copyMidiDataDefensively() {
        // given
        Playing playing = createInProgressPlaying();

        List<MidiEventData> midiData = new ArrayList<>();
        midiData.add(createMidiEvent(0, 0L));
        midiData.add(createMidiEvent(1, 100L));

        // when
        playing.completeWithMidiData(midiData, RECORDING_OBJECT_KEY);
        midiData.clear();

        // then
        assertThat(playing.getMidiData())
                .hasSize(2);
    }

    @Test
    @DisplayName("getMidiData로 반환된 목록은 수정할 수 없다")
    void returnUnmodifiableMidiData() {
        // given
        Playing playing = createInProgressPlaying();

        playing.completeWithMidiData(
                List.of(createMidiEvent(0, 0L)),
                RECORDING_OBJECT_KEY
        );

        List<MidiEventData> savedMidiData =
                playing.getMidiData();

        // when & then
        assertThatThrownBy(
                () -> savedMidiData.add(
                        createMidiEvent(1, 100L)
                )
        )
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("사용자가 null이면 MISSING_USER_ID 예외가 발생한다")
    void rejectNullUser() {
        BackingTrack backingTrack =
                mock(BackingTrack.class);

        assertThatThrownBy(() ->
                Playing.createBackingTrack(
                        null,
                        backingTrack,
                        BPM
                )
        )
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> {
                    GeneralException generalException =
                            (GeneralException) exception;

                    assertThat(generalException.getCode())
                            .isEqualTo(
                                    PlayingErrorStatus.MISSING_USER_ID
                            );
                });
    }

    @Test
    @DisplayName("연주 소유자의 사용자 ID가 일치하면 검증을 통과한다")
    void validatePlayingOwner_success() {
        User user = mock(User.class);
        BackingTrack backingTrack =
                mock(BackingTrack.class);

        when(user.getUserId())
                .thenReturn(1L);

        Playing playing =
                Playing.createBackingTrack(
                        user,
                        backingTrack,
                        BPM
                );

        assertThatCode(() ->
                playing.validatePlayingOwner(1L)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("연주 소유자의 사용자 ID가 다르면 접근 예외가 발생한다")
    void validatePlayingOwner_accessDenied() {
        User user = mock(User.class);
        BackingTrack backingTrack =
                mock(BackingTrack.class);

        when(user.getUserId())
                .thenReturn(1L);

        Playing playing =
                Playing.createBackingTrack(
                        user,
                        backingTrack,
                        BPM
                );

        assertThatThrownBy(() ->
                playing.validatePlayingOwner(2L)
        )
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> {
                    GeneralException generalException =
                            (GeneralException) exception;

                    assertThat(generalException.getCode())
                            .isEqualTo(
                                    PlayingErrorStatus.PLAYING_ACCESS_DENIED
                            );
                });
    }

    @Test
    @DisplayName("백킹트랙이 null이면 MISSING_BACKING_TRACK_ID 예외가 발생한다")
    void rejectNullBackingTrack() {
        User user = mock(User.class);

        assertThatThrownBy(() ->
                Playing.createBackingTrack(
                        user,
                        null,
                        BPM
                )
        )
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> {
                    GeneralException generalException =
                            (GeneralException) exception;

                    assertThat(generalException.getCode())
                            .isEqualTo(
                                    PlayingErrorStatus.MISSING_BACKING_TRACK_ID
                            );
                });
    }

    @Test
    @DisplayName("READY 상태의 연주를 시작하면 IN_PROGRESS 상태가 되고 시작 시간이 기록된다")
    void startPlaying_success() {
        // given
        User user = mock(User.class);
        BackingTrack backingTrack = mock(BackingTrack.class);

        Playing playing = Playing.createBackingTrack(
                user,
                backingTrack,
                120
        );

        LocalDateTime beforeStart = LocalDateTime.now();

        // when
        playing.start();

        LocalDateTime afterStart = LocalDateTime.now();

        // then
        assertThat(playing.getStatus())
                .isEqualTo(PlayingStatus.IN_PROGRESS);

        assertThat(playing.getStartedAt())
                .isNotNull()
                .isBetween(beforeStart, afterStart);
    }

    @Test
    @DisplayName("IN_PROGRESS 상태의 연주를 다시 시작하면 예외가 발생한다")
    void startPlaying_invalidStatus() {
        // given
        User user = mock(User.class);
        BackingTrack backingTrack = mock(BackingTrack.class);

        Playing playing = Playing.createBackingTrack(
                user,
                backingTrack,
                120
        );

        playing.start();

        // when & then
        assertThatThrownBy(playing::start)
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> {
                    GeneralException generalException =
                            (GeneralException) exception;

                    assertThat(generalException.getCode())
                            .isEqualTo(
                                    PlayingErrorStatus.INVALID_PLAYING_STATUS
                            );
                });
    }

    @Test
    @DisplayName("백킹트랙 연주를 생성하면 READY 상태와 비공개 설정이 적용된다")
    void createBackingTrackPlaying_success() {
        // given
        User user = mock(User.class);
        BackingTrack backingTrack = mock(BackingTrack.class);

        // when
        Playing playing = Playing.createBackingTrack(
                user,
                backingTrack,
                120
        );

        // then
        assertThat(playing.getUser())
                .isEqualTo(user);

        assertThat(playing.getBackingTrack())
                .isEqualTo(backingTrack);

        assertThat(playing.getMode())
                .isEqualTo(PlayingMode.BACKING_TRACK);

        assertThat(playing.getStatus())
                .isEqualTo(PlayingStatus.READY);

        assertThat(playing.getBpm())
                .isEqualTo(120);

        assertThat(playing.isPublic())
                .isFalse();

        assertThat(playing.getStartedAt())
                .isNull();
    }

    @Test
    @DisplayName("진행 중인 연주에 MIDI 데이터와 녹음 파일 Object Key를 저장하면 완료 상태로 변경된다")
    void completeWithMidiDataAndRecordingObjectKey() {
        // given
        Playing playing = createInProgressPlaying();

        List<MidiEventData> midiData = List.of(
                createMidiEvent(0, 100L),
                createMidiEvent(1, 200L),
                createMidiEvent(2, 300L)
        );

        // when
        playing.completeWithMidiData(
                midiData,
                RECORDING_OBJECT_KEY
        );

        // then
        assertThat(playing.getMidiData())
                .hasSize(3);

        assertThat(playing.getRecordingObjectKey())
                .isEqualTo(RECORDING_OBJECT_KEY);

        assertThat(playing.getEndedAt())
                .isNotNull();

        assertThat(playing.getDurationSec())
                .isNotNull()
                .isGreaterThanOrEqualTo(0)
                .isLessThanOrEqualTo(600);

        assertThat(playing.getStatus())
                .isEqualTo(PlayingStatus.COMPLETED);
    }

    private Playing createReadyPlaying() {
        User user = mock(User.class);
        BackingTrack backingTrack = mock(BackingTrack.class);

        when(backingTrack.getPlaytimeSec())
                .thenReturn(600);

        return Playing.createBackingTrack(
                user,
                backingTrack,
                BPM
        );
    }

    private Playing createInProgressPlaying() {
        Playing playing = createReadyPlaying();
        playing.start();

        ReflectionTestUtils.setField(
                playing,
                "startedAt",
                LocalDateTime.now().minusSeconds(1)
        );

        return playing;
    }

    private MidiEventData createMidiEvent(
            int sequence,
            long timestampMs
    ) {
        return MidiEventData.of(
                sequence,
                MidiType.NOTE_ON,
                60,
                100,
                timestampMs
        );
    }
}