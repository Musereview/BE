package com.mr.domain.playing.service;

import com.mr.domain.playing.dto.req.MidiEventSaveRequest;
import com.mr.domain.playing.dto.res.MidiEventSaveResponse;
import com.mr.domain.playing.entity.MidiEventData;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.MidiType;
import com.mr.domain.playing.exception.MidiEventErrorStatus;
import com.mr.domain.playing.exception.PlayingErrorStatus;
import com.mr.domain.playing.repository.PlayingRepository;
import com.mr.global.apipayload.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayingServiceTest {

    @Mock
    private PlayingRepository playingRepository;

    @Mock
    private Playing playing;

    @InjectMocks
    private PlayingService playingService;

    private Long userId;
    private Long playingId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        playingId = 10L;
    }

    @Nested
    @DisplayName("MIDI 이벤트 저장")
    class SaveMidiEvents {

        @Test
        @DisplayName("진행 중인 본인의 연주에 MIDI 이벤트를 저장한다")
        void saveMidiEvents_success() {
            // given
            MidiEventSaveRequest request = createRequest();

            when(playingRepository.findById(playingId))
                    .thenReturn(Optional.of(playing));

            when(playing.getId())
                    .thenReturn(playingId);

            /*
             * Playing을 Mock으로 사용하면 completeWithMidiData()가 실제로
             * midiData를 저장하지 않으므로, 전달받은 값을 getMidiData()가
             * 반환하도록 구성합니다.
             */
            doAnswer(invocation -> {
                List<MidiEventData> midiEvents =
                        invocation.getArgument(0);

                when(playing.getMidiData())
                        .thenReturn(midiEvents);

                return null;
            }).when(playing).completeWithMidiData(anyList());

            // when
            MidiEventSaveResponse response =
                    playingService.saveMidiEvents(
                            userId,
                            playingId,
                            request
                    );

            // then
            assertThat(response.playingId())
                    .isEqualTo(playingId);

            assertThat(response.savedCount())
                    .isEqualTo(2);

            verify(playingRepository)
                    .findById(playingId);

            verify(playing)
                    .validateOwner(userId);

            verify(playing)
                    .completeWithMidiData(anyList());
        }

        @Test
        @DisplayName("요청 DTO의 MIDI 이벤트를 엔티티 값 객체로 변환한다")
        void saveMidiEvents_convertsRequestToEntity() {
            // given
            MidiEventSaveRequest request = createRequest();

            when(playingRepository.findById(playingId))
                    .thenReturn(Optional.of(playing));

            when(playing.getId())
                    .thenReturn(playingId);

            doAnswer(invocation -> {
                List<MidiEventData> midiEvents =
                        invocation.getArgument(0);

                assertThat(midiEvents)
                        .hasSize(2);

                MidiEventData firstEvent = midiEvents.get(0);

                assertThat(firstEvent.getSequence())
                        .isZero();

                assertThat(firstEvent.getType())
                        .isEqualTo(MidiType.NOTE_ON);

                assertThat(firstEvent.getPitch())
                        .isEqualTo(60);

                assertThat(firstEvent.getVelocity())
                        .isEqualTo(100);

                assertThat(firstEvent.getTimestampMs())
                        .isEqualTo(0L);

                when(playing.getMidiData())
                        .thenReturn(midiEvents);

                return null;
            }).when(playing).completeWithMidiData(anyList());

            // when
            playingService.saveMidiEvents(
                    userId,
                    playingId,
                    request
            );

            // then
            verify(playing)
                    .completeWithMidiData(anyList());
        }

        @Test
        @DisplayName("playingId가 null이면 예외가 발생한다")
        void saveMidiEvents_nullPlayingId() {
            // given
            MidiEventSaveRequest request = createRequest();

            // when & then
            assertThatThrownBy(() ->
                    playingService.saveMidiEvents(
                            userId,
                            null,
                            request
                    )
            )
                    .isInstanceOf(GeneralException.class)
                    .satisfies(exception -> {
                        GeneralException generalException =
                                (GeneralException) exception;

                        assertThat(generalException.getCode())
                                .isEqualTo(
                                        MidiEventErrorStatus
                                                .INVALID_PLAYING_ID
                                );
                    });

            verify(playingRepository, never())
                    .findById(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("playingId가 0 이하이면 예외가 발생한다")
        void saveMidiEvents_invalidPlayingId() {
            // given
            MidiEventSaveRequest request = createRequest();

            // when & then
            assertThatThrownBy(() ->
                    playingService.saveMidiEvents(
                            userId,
                            0L,
                            request
                    )
            )
                    .isInstanceOf(GeneralException.class)
                    .satisfies(exception -> {
                        GeneralException generalException =
                                (GeneralException) exception;

                        assertThat(generalException.getCode())
                                .isEqualTo(
                                        MidiEventErrorStatus
                                                .INVALID_PLAYING_ID
                                );
                    });

            verify(playingRepository, never())
                    .findById(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("연주가 존재하지 않으면 예외가 발생한다")
        void saveMidiEvents_playingNotFound() {
            // given
            MidiEventSaveRequest request = createRequest();

            when(playingRepository.findById(playingId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    playingService.saveMidiEvents(
                            userId,
                            playingId,
                            request
                    )
            )
                    .isInstanceOf(GeneralException.class)
                    .satisfies(exception -> {
                        GeneralException generalException =
                                (GeneralException) exception;

                        assertThat(generalException.getCode())
                                .isEqualTo(
                                        PlayingErrorStatus
                                                .PLAYING_NOT_FOUND
                                );
                    });

            verify(playing, never())
                    .validateOwner(userId);

            verify(playing, never())
                    .completeWithMidiData(anyList());
        }
    }

    private MidiEventSaveRequest createRequest() {
        List<MidiEventSaveRequest.MidiEventRequest> events =
                List.of(
                        new MidiEventSaveRequest.MidiEventRequest(
                                0,
                                MidiType.NOTE_ON,
                                60,
                                100,
                                0L
                        ),
                        new MidiEventSaveRequest.MidiEventRequest(
                                1,
                                MidiType.NOTE_OFF,
                                60,
                                0,
                                500L
                        )
                );

        return new MidiEventSaveRequest(events);
    }
}