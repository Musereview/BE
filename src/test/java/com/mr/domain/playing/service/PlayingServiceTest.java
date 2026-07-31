package com.mr.domain.playing.service;

import com.mr.domain.backingTrack.entity.BackingTrack;
import com.mr.domain.backingTrack.entity.enums.AccessLevel;
import com.mr.domain.backingTrack.repository.BackingTrackRepository;
import com.mr.domain.playing.dto.req.MidiEventSaveRequest;
import com.mr.domain.playing.dto.req.PlayingStartRequest;
import com.mr.domain.playing.dto.res.MidiEventSaveResponse;
import com.mr.domain.playing.dto.res.PlayingStartResponse;
import com.mr.domain.playing.entity.MidiEventData;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.MidiType;
import com.mr.domain.playing.entity.enums.PlayingMode;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import com.mr.domain.playing.exception.MidiEventErrorStatus;
import com.mr.domain.playing.exception.PlayingErrorStatus;
import com.mr.domain.playing.repository.PlayingRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class PlayingServiceTest {

    private static final Integer BPM = 120;

    @Mock
    private PlayingRepository playingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BackingTrackRepository backingTrackRepository;

    @Mock
    private Playing playing;

    private User user;
    private BackingTrack backingTrack;

    @InjectMocks
    private PlayingService playingService;

    private Long userId;
    private Long playingId;
    private Long backingTrackId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        playingId = 10L;
        backingTrackId = 11L;

        user = mock(User.class);
        backingTrack = mock(BackingTrack.class);
    }

    @Nested
    @DisplayName("MIDI 이벤트 저장")
    class SaveMidiEvents {

        @Test
        @DisplayName("진행 중인 본인의 연주에 MIDI 이벤트를 저장한다")
        void saveMidiEvents_success() {
            // given
            MidiEventSaveRequest request = createRequest();

            when(playingRepository.findByIdAndDeletedAtIsNull(playingId))
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
                    .findByIdAndDeletedAtIsNull(playingId);

            verify(playing)
                    .validatePlayingOwner(userId);

            verify(playing)
                    .completeWithMidiData(anyList());
        }

        @Test
        @DisplayName("본인의 연주가 아니면 MIDI 이벤트를 저장하지 않는다")
        void saveMidiEvents_accessDenied() {
            MidiEventSaveRequest request =
                    createRequest();

            when(
                    playingRepository
                            .findByIdAndDeletedAtIsNull(playingId)
            )
                    .thenReturn(Optional.of(playing));

            doThrow(
                    new GeneralException(
                            PlayingErrorStatus.PLAYING_ACCESS_DENIED
                    )
            )
                    .when(playing)
                    .validatePlayingOwner(userId);

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
                                        PlayingErrorStatus.PLAYING_ACCESS_DENIED
                                );
                    });

            verify(playing, never())
                    .completeWithMidiData(anyList());
        }

        @Test
        @DisplayName("요청 DTO의 MIDI 이벤트를 엔티티 값 객체로 변환한다")
        void saveMidiEvents_convertsRequestToEntity() {
            // given
            MidiEventSaveRequest request = createRequest();

            when(playingRepository.findByIdAndDeletedAtIsNull(playingId))
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
        @DisplayName("진행 중이 아닌 연주에는 MIDI 이벤트를 저장할 수 없다")
        void saveMidiEvents_notInProgress() {
            MidiEventSaveRequest request =
                    createRequest();

            when(
                    playingRepository
                            .findByIdAndDeletedAtIsNull(playingId)
            )
                    .thenReturn(Optional.of(playing));

            doThrow(
                    new GeneralException(
                            MidiEventErrorStatus.PLAYING_NOT_IN_PROGRESS
                    )
            )
                    .when(playing)
                    .completeWithMidiData(anyList());

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
                                        MidiEventErrorStatus.PLAYING_NOT_IN_PROGRESS
                                );
                    });
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
                    .findByIdAndDeletedAtIsNull(any());
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
                    .findByIdAndDeletedAtIsNull(any());
        }

        @Test
        @DisplayName("연주가 존재하지 않으면 예외가 발생한다")
        void saveMidiEvents_playingNotFound() {
            // given
            MidiEventSaveRequest request = createRequest();

            when(playingRepository.findByIdAndDeletedAtIsNull(playingId))
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
                    .validatePlayingOwner(userId);

            verify(playing, never())
                    .completeWithMidiData(anyList());
        }
    }

    @Nested
    @DisplayName("연주 세션 시작")
    class PlayingStart {

        @Test
        @DisplayName("사용자와 백킹트랙이 존재하면 연주 세션을 시작한다")
        void startPlaying_success() {
            // given
            PlayingStartRequest request =
                    new PlayingStartRequest(backingTrackId);

            given(userRepository.findById(userId))
                    .willReturn(Optional.of(user));

            given(backingTrackRepository.findById(backingTrackId))
                    .willReturn(Optional.of(backingTrack));

            given(backingTrack.getId())
                    .willReturn(backingTrackId);

            given(backingTrack.getAccessLevel())
                    .willReturn(AccessLevel.PUBLIC);

            given(backingTrack.getBpm())
                    .willReturn(BPM);

            given(backingTrack.getTitle())
                    .willReturn("테스트 백킹트랙");

            given(backingTrack.getAudioFileUrl())
                    .willReturn("https://example.com/backing-track.mp3");

            given(backingTrack.getChordProgressions())
                    .willReturn(List.of());

            given(playingRepository.save(any(Playing.class)))
                    .willAnswer(invocation -> {
                        Playing playing = invocation.getArgument(0);

                        ReflectionTestUtils.setField(
                                playing,
                                "id",
                                playingId
                        );

                        return playing;
                    });

            // when
            PlayingStartResponse response =
                    playingService.startPlaying(userId, request);

            // then
            assertThat(response.playingId())
                    .isEqualTo(playingId);

            assertThat(response.status())
                    .isEqualTo(PlayingStatus.IN_PROGRESS);

            assertThat(response.startedAt())
                    .isNotNull();

            assertThat(response.backingTrack())
                    .isNotNull();

            assertThat(response.backingTrack().backingTrackId())
                    .isEqualTo(backingTrackId);

            ArgumentCaptor<Playing> playingCaptor =
                    ArgumentCaptor.forClass(Playing.class);

            verify(playingRepository)
                    .save(playingCaptor.capture());

            Playing savedPlaying = playingCaptor.getValue();

            assertThat(savedPlaying.getUser())
                    .isSameAs(user);

            assertThat(savedPlaying.getBackingTrack())
                    .isSameAs(backingTrack);

            assertThat(savedPlaying.getMode())
                    .isEqualTo(PlayingMode.BACKING_TRACK);

            assertThat(savedPlaying.getStatus())
                    .isEqualTo(PlayingStatus.IN_PROGRESS);

            assertThat(savedPlaying.getBpm())
                    .isEqualTo(BPM);

            assertThat(savedPlaying.getStartedAt())
                    .isNotNull();
        }

        @Test
        @DisplayName("사용자가 존재하지 않으면 연주 세션을 생성하지 않는다")
        void startPlaying_userNotFound() {
            // given
            PlayingStartRequest request = new PlayingStartRequest(backingTrackId);
            given(userRepository.findById(userId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    playingService.startPlaying(
                            userId,
                            request
                    )
            )
                    .isInstanceOf(GeneralException.class);

            verify(backingTrackRepository, never())
                    .findById(any());

            verify(playingRepository, never())
                    .save(any());
        }

        @Test
        @DisplayName("백킹트랙이 존재하지 않으면 연주 세션을 생성하지 않는다")
        void startPlaying_backingTrackNotFound() {
            // given
            PlayingStartRequest request = new PlayingStartRequest(backingTrackId);
            given(userRepository.findById(userId))
                    .willReturn(Optional.of(user));

            given(backingTrackRepository.findById(backingTrackId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    playingService.startPlaying(
                            userId,
                            request
                    )
            )
                    .isInstanceOf(GeneralException.class);

            verify(playingRepository, never())
                    .save(any());
        }

        @Test
        @DisplayName("사용자 ID가 null이면 연주 세션을 생성하지 않는다")
        void startPlaying_nullUserId() {
            // given
            PlayingStartRequest request = new PlayingStartRequest(backingTrackId);

            // when & then
            assertThatThrownBy(() ->
                    playingService.startPlaying(
                            null,
                            request
                    )
            )
                    .isInstanceOf(GeneralException.class);

            verify(userRepository, never())
                    .findById(any());

            verify(backingTrackRepository, never())
                    .findById(any());

            verify(playingRepository, never())
                    .save(any());
        }

        @Test
        @DisplayName("백킹트랙 ID가 null이면 연주 세션을 생성하지 않는다")
        void startPlaying_nullBackingTrackId() {

            // given
            PlayingStartRequest request = new PlayingStartRequest(null); // 백킹트랙 ID만 null인 DTO 생성

            // when & then
            assertThatThrownBy(() ->
                    playingService.startPlaying(
                            userId,
                            request
                    )
            )
                    .isInstanceOf(GeneralException.class);

            verify(playingRepository, never())
                    .save(any());
        }

        private User mockUser() {
            User user = mock(User.class);

            given(user.getUserId())
                    .willReturn(userId);

            return user;
        }

        private BackingTrack mockBackingTrack() {
            BackingTrack backingTrack =
                    mock(BackingTrack.class);

            given(backingTrack.getId())
                    .willReturn(backingTrackId);

            given(backingTrack.getTitle())
                    .willReturn("Blues Backing Track");

            given(backingTrack.getAudioFileUrl())
                    .willReturn("https://example.com/backing-track.mp3");

            given(backingTrack.getGenre())
                    .willReturn("BLUES");

            given(backingTrack.getKeySignature())
                    .willReturn("C");

            given(backingTrack.getBpm())
                    .willReturn(BPM);

            given(backingTrack.getTimeSignature())
                    .willReturn("4/4");

            given(backingTrack.getPlaytimeSec())
                    .willReturn(180);

            given(backingTrack.getChordProgressions())
                    .willReturn(List.of());

            return backingTrack;
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