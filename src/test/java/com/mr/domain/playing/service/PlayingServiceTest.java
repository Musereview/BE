package com.mr.domain.playing.service;

import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.analysis.service.AnalysisBarCalculator;
import com.mr.domain.backingtrack.entity.enums.AccessLevel;
import com.mr.domain.backingtrack.repository.BackingTrackRepository;
import com.mr.domain.playing.dto.req.MidiEventSaveRequest;
import com.mr.domain.playing.dto.req.PlayingStartRequest;
import com.mr.domain.playing.dto.req.RecordingUploadUrlRequest;
import com.mr.domain.playing.dto.res.MidiEventSaveResponse;
import com.mr.domain.playing.dto.res.AnalysisContextResponse;
import com.mr.domain.playing.dto.res.PlayingDeleteResponse;
import com.mr.domain.playing.dto.res.PlayingDetailResponse;
import com.mr.domain.playing.dto.res.PlayingStartResponse;
import com.mr.domain.playing.dto.res.RecordingUploadUrlResponse;
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
import com.mr.global.event.PlayingCompletedEvent;
import com.mr.global.file.s3.dto.FileUploadCommand;
import com.mr.global.file.s3.dto.ValidatedFile;
import com.mr.global.file.s3.dto.PresignedUrlUpload;
import com.mr.global.file.s3.enums.S3FileType;
import com.mr.global.file.s3.service.S3FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayingServiceTest {

    private static final Integer BPM = 120;
    private static final String RECORDING_OBJECT_KEY =
            "recordings/1/2026-08-02/150000_a1b2c3.mp3";
    private static final String RECORDING_FILE_URL =
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/"
                    + RECORDING_OBJECT_KEY;
    private static final String BACKING_TRACK_OBJECT_KEY =
            "backing-tracks/1/2026-08-02/150000_a1b2c3.mp3";
    private static final String BACKING_TRACK_FILE_URL =
            "https://example.com/presigned-backing-track.mp3";

    @Mock
    private PlayingRepository playingRepository;

    @Mock
    private AnalysisBarCalculator analysisBarCalculator;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BackingTrackRepository backingTrackRepository;

    @Mock
    private Playing playing;

    @Mock
    private S3FileService s3FileService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Clock clock;

    private User user;
    private BackingTrack backingTrack;

    @InjectMocks
    private PlayingService playingService;

    private Long userId;
    private Long playingId;
    private Long backingTrackId;

    ValidatedFile recordingResponse =
            new ValidatedFile(
                    RECORDING_OBJECT_KEY,
                    1_157_632L,
                    "audio/mpeg"
            );

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

            stubTransactionExecution();
            stubCompletedPlayingForMilestoneCalculation();

            when(s3FileService.validateUploadedFile(
                    userId,
                    S3FileType.RECORDING,
                    RECORDING_OBJECT_KEY
            )).thenReturn(recordingResponse);

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
            }).when(playing).completeWithMidiData(anyList(), eq(RECORDING_OBJECT_KEY));

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

            verify(s3FileService)
                    .validateUploadedFile(
                            userId,
                            S3FileType.RECORDING,
                            RECORDING_OBJECT_KEY
                    );

            verify(playing)
                    .completeWithMidiData(anyList(), eq(RECORDING_OBJECT_KEY));

            ArgumentCaptor<Object> eventCaptor =
                    ArgumentCaptor.forClass(Object.class);

            verify(eventPublisher)
                    .publishEvent(eventCaptor.capture());

            assertThat(eventCaptor.getValue())
                    .isInstanceOfSatisfying(
                            PlayingCompletedEvent.class,
                            event -> assertThat(event.getUserId())
                                    .isEqualTo(userId)
                    );
        }

        @Test
        @DisplayName("본인의 연주가 아니면 MIDI 이벤트를 저장하지 않는다")
        void saveMidiEvents_accessDenied() {
            MidiEventSaveRequest request =
                    createRequest();

            stubTransactionExecution();

            when(s3FileService.validateUploadedFile(
                    userId,
                    S3FileType.RECORDING,
                    RECORDING_OBJECT_KEY
            )).thenReturn(recordingResponse);

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

            verify(s3FileService)
                    .validateUploadedFile(
                            userId,
                            S3FileType.RECORDING,
                            RECORDING_OBJECT_KEY
                    );

            verify(playingRepository)
                    .findByIdAndDeletedAtIsNull(playingId);

            verify(playing)
                    .validatePlayingOwner(userId);

            verify(playing, never())
                    .validateInProgress();

            verify(playing, never())
                    .completeWithMidiData(
                            anyList(),
                            anyString()
                    );

            verify(eventPublisher, never())
                    .publishEvent(any());
        }

        @Test
        @DisplayName("요청 DTO의 MIDI 이벤트를 엔티티 값 객체로 변환한다")
        void saveMidiEvents_convertsRequestToEntity() {
            // given
            MidiEventSaveRequest request = createRequest();

            stubTransactionExecution();
            stubCompletedPlayingForMilestoneCalculation();

            when(s3FileService.validateUploadedFile(
                    userId,
                    S3FileType.RECORDING,
                    RECORDING_OBJECT_KEY
            )).thenReturn(recordingResponse);

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
            }).when(playing).completeWithMidiData(anyList(), eq(RECORDING_OBJECT_KEY));

            // when
            playingService.saveMidiEvents(
                    userId,
                    playingId,
                    request
            );

            // then
            verify(s3FileService)
                    .validateUploadedFile(
                            userId,
                            S3FileType.RECORDING,
                            RECORDING_OBJECT_KEY
                    );

            verify(playing)
                    .validatePlayingOwner(userId);

            verify(playing)
                    .validateInProgress();

            verify(playing)
                    .completeWithMidiData(
                            anyList(),
                            eq(RECORDING_OBJECT_KEY)
                    );

            verify(eventPublisher)
                    .publishEvent(any(PlayingCompletedEvent.class));
        }

        @Test
        @DisplayName("진행 중이 아닌 연주에는 MIDI 이벤트를 저장할 수 없다")
        void saveMidiEvents_notInProgress() {
            MidiEventSaveRequest request =
                    createRequest();

            stubTransactionExecution();

            when(s3FileService.validateUploadedFile(
                    userId,
                    S3FileType.RECORDING,
                    RECORDING_OBJECT_KEY
            )).thenReturn(recordingResponse);

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
                    .validateInProgress();

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

            verify(s3FileService)
                    .validateUploadedFile(
                            userId,
                            S3FileType.RECORDING,
                            RECORDING_OBJECT_KEY
                    );

            verify(playingRepository)
                    .findByIdAndDeletedAtIsNull(playingId);

            verify(playing)
                    .validatePlayingOwner(userId);

            verify(playing)
                    .validateInProgress();

            verify(playing, never())
                    .completeWithMidiData(anyList(), anyString());

            verify(eventPublisher, never())
                    .publishEvent(any());
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
                                        PlayingErrorStatus
                                                .INVALID_PLAYING_ID
                                );
                    });

            verify(s3FileService, never())
                    .validateUploadedFile(
                            anyLong(),
                            any(S3FileType.class),
                            anyString()
                    );

            verify(playingRepository, never())
                    .findByIdAndDeletedAtIsNull(any());

            verify(eventPublisher, never())
                    .publishEvent(any());
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
                                        PlayingErrorStatus
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

            stubTransactionExecution();

            when(s3FileService.validateUploadedFile(
                    userId,
                    S3FileType.RECORDING,
                    RECORDING_OBJECT_KEY
            )).thenReturn(recordingResponse);

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

            verify(s3FileService)
                    .validateUploadedFile(
                            userId,
                            S3FileType.RECORDING,
                            RECORDING_OBJECT_KEY
                    );

            verify(playingRepository)
                    .findByIdAndDeletedAtIsNull(playingId);

            verify(playing, never())
                    .validatePlayingOwner(userId);

            verify(playing, never())
                    .completeWithMidiData(anyList(), anyString());

            verify(eventPublisher, never())
                    .publishEvent(any());
        }

        private void stubTransactionExecution() {
            doAnswer(invocation -> {
                TransactionCallback<?> callback =
                        invocation.getArgument(0);

                return callback.doInTransaction(
                        mock(TransactionStatus.class)
                );
            })
                    .when(transactionTemplate)
                    .execute(any());
        }

        private void stubCompletedPlayingForMilestoneCalculation() {
            LocalDate fixedDate = LocalDate.of(2026, 8, 4);
            ZoneId zoneId = ZoneId.of("Asia/Seoul");
            Instant fixedInstant = fixedDate
                    .atStartOfDay(zoneId)
                    .toInstant();

            // lenient() 를 붙여서 불필요한 스터빙 에러를 방지합니다.
            org.mockito.Mockito.lenient().when(clock.instant())
                    .thenReturn(fixedInstant);

            org.mockito.Mockito.lenient().when(clock.getZone())
                    .thenReturn(zoneId);

            org.mockito.Mockito.lenient().when(playing.getStatus())
                    .thenReturn(PlayingStatus.COMPLETED);

            org.mockito.Mockito.lenient().when(playing.getDurationSec())
                    .thenReturn(300);

            Instant weekStart = fixedDate
                    .with(DayOfWeek.MONDAY)
                    .atStartOfDay(ZoneId.of("Asia/Seoul"))
                    .toInstant();

            org.mockito.Mockito.lenient().when(playingRepository.sumDurationSecExcludeCurrent(
                    userId,
                    PlayingStatus.COMPLETED,
                    weekStart,
                    playingId
            )).thenReturn(0L);
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

            given(backingTrackRepository.findByIdWithChordProgressions(backingTrackId))
                    .willReturn(Optional.of(backingTrack));

            given(backingTrack.getId())
                    .willReturn(backingTrackId);

            given(backingTrack.getAccessLevel())
                    .willReturn(AccessLevel.PUBLIC);

            given(backingTrack.getBpm())
                    .willReturn(BPM);

            given(backingTrack.getTitle())
                    .willReturn("테스트 백킹트랙");

            given(backingTrack.getAudioObjectKey())
                    .willReturn(BACKING_TRACK_OBJECT_KEY);

            given(backingTrack.getUser())
                    .willReturn(user);

            given(user.getUserId())
                    .willReturn(userId);

            given(s3FileService.createPresignedDownload(
                    userId,
                    S3FileType.BACKING_TRACK,
                    BACKING_TRACK_OBJECT_KEY
            )).willReturn(BACKING_TRACK_FILE_URL);

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

            given(backingTrackRepository.findByIdWithChordProgressions(backingTrackId))
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

            given(backingTrack.getAudioObjectKey())
                    .willReturn(BACKING_TRACK_OBJECT_KEY);

            given(backingTrack.getUser())
                    .willReturn(user);

            given(user.getUserId())
                    .willReturn(userId);

            given(s3FileService.createPresignedDownload(
                    userId,
                    S3FileType.BACKING_TRACK,
                    BACKING_TRACK_OBJECT_KEY
            )).willReturn(BACKING_TRACK_FILE_URL);

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

        return new MidiEventSaveRequest(events, RECORDING_OBJECT_KEY);
    }

    @Nested
    @DisplayName("연주 세션 단건 조회")
    class GetPlayingDetail {

        @Test
        @DisplayName("본인의 완료된 연주 세션을 조회한다")
        void getPlayingDetailSuccess() {

            when(playingRepository.findByIdWithBackingTrack(playingId))
                    .thenReturn(Optional.of(playing));

            when(playing.getId()).thenReturn(playingId);
            when(playing.getStatus()).thenReturn(PlayingStatus.COMPLETED);

            PlayingDetailResponse response =
                    playingService.getPlayingDetail(userId, playingId);

            assertThat(response.playingId()).isEqualTo(playingId);
            assertThat(response.status()).isEqualTo(PlayingStatus.COMPLETED);

            verify(playing).validatePlayingOwner(userId);
            verify(playing).validateCompleted();
        }

        @Test
        @DisplayName("연주 세션 ID가 1 미만이면 예외가 발생한다")
        void invalidPlayingId() {
            assertThatThrownBy(() ->
                    playingService.getPlayingDetail(1L, 0L)
            )
                    .isInstanceOf(GeneralException.class)
                    .satisfies(exception -> {
                        GeneralException generalException =
                                (GeneralException) exception;

                        assertThat(generalException.getCode())
                                .isEqualTo(PlayingErrorStatus.INVALID_PLAYING_ID);
                    });
        }

        @Test
        @DisplayName("연주 세션이 존재하지 않으면 예외가 발생한다")
        void playingNotFound() {
            // given
            Long playingId = 10L;

            given(playingRepository.findByIdWithBackingTrack(playingId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    playingService.getPlayingDetail(1L, playingId)
            )
                    .isInstanceOf(GeneralException.class);
        }

        @Test
        @DisplayName("다른 사용자의 연주 세션이면 예외가 발생한다")
        void playingAccessDenied() {

            when(playingRepository.findByIdWithBackingTrack(playingId))
                    .thenReturn(Optional.of(playing));

            doThrow(new GeneralException(
                    PlayingErrorStatus.PLAYING_ACCESS_DENIED))
                    .when(playing)
                    .validatePlayingOwner(userId);

            assertThatThrownBy(() ->
                    playingService.getPlayingDetail(userId, playingId))
                    .isInstanceOf(GeneralException.class);

            verify(playing, never()).validateCompleted();
        }

        @Test
        @DisplayName("완료되지 않은 연주 세션이면 예외가 발생한다")
        void playingNotCompleted() {

            when(playingRepository.findByIdWithBackingTrack(playingId))
                    .thenReturn(Optional.of(playing));

            doThrow(new GeneralException(
                    PlayingErrorStatus.PLAYING_NOT_COMPLETED))
                    .when(playing)
                    .validateCompleted();

            assertThatThrownBy(() ->
                    playingService.getPlayingDetail(userId, playingId))
                    .isInstanceOf(GeneralException.class);
        }
    }

    @Nested
    @DisplayName("분석 마디 선택 정보 조회")
    class GetAnalysisContext {

        @Test
        @DisplayName("본인의 완료된 연주와 전체 마디 수를 조회한다")
        void getAnalysisContextSuccess() {
            when(playingRepository.findByIdWithBackingTrack(playingId))
                    .thenReturn(Optional.of(playing));
            when(playing.getBackingTrack()).thenReturn(backingTrack);
            when(playing.getId()).thenReturn(playingId);
            when(playing.getBpm()).thenReturn(BPM);
            when(playing.getMidiData()).thenReturn(List.of());
            when(backingTrack.getTimeSignature()).thenReturn("4/4");
            when(analysisBarCalculator.calculate(playing))
                    .thenReturn(new AnalysisBarCalculator.BarMetrics(
                            new int[]{4, 4},
                            2_000D,
                            60
                    ));

            AnalysisContextResponse response =
                    playingService.getAnalysisContext(userId, playingId);

            assertThat(response.playingId()).isEqualTo(playingId);
            assertThat(response.totalBars()).isEqualTo(60);
            verify(playing).validatePlayingOwner(userId);
            verify(playing).validateCompleted();
        }

        @Test
        @DisplayName("백킹트랙이 연결되지 않으면 예외가 발생한다")
        void backingTrackNotFound() {
            when(playingRepository.findByIdWithBackingTrack(playingId))
                    .thenReturn(Optional.of(playing));
            when(playing.getBackingTrack()).thenReturn(null);

            assertThatThrownBy(() ->
                    playingService.getAnalysisContext(userId, playingId)
            )
                    .isInstanceOf(GeneralException.class)
                    .hasFieldOrPropertyWithValue(
                            "code",
                            PlayingErrorStatus.BACKING_TRACK_NOT_FOUND
                    );

            verify(analysisBarCalculator, never()).calculate(any());
        }

        @Test
        @DisplayName("완료되지 않은 연주는 분석 정보를 조회할 수 없다")
        void playingNotCompleted() {
            when(playingRepository.findByIdWithBackingTrack(playingId))
                    .thenReturn(Optional.of(playing));
            doThrow(new GeneralException(PlayingErrorStatus.PLAYING_NOT_COMPLETED))
                    .when(playing)
                    .validateCompleted();

            assertThatThrownBy(() ->
                    playingService.getAnalysisContext(userId, playingId)
            )
                    .isInstanceOf(GeneralException.class)
                    .hasFieldOrPropertyWithValue(
                            "code",
                            PlayingErrorStatus.PLAYING_NOT_COMPLETED
                    );

            verify(analysisBarCalculator, never()).calculate(any());
        }

        @Test
        @DisplayName("다른 사용자의 연주는 조회할 수 없다")
        void playingAccessDenied() {
            when(playingRepository.findByIdWithBackingTrack(playingId))
                    .thenReturn(Optional.of(playing));
            doThrow(new GeneralException(PlayingErrorStatus.PLAYING_ACCESS_DENIED))
                    .when(playing)
                    .validatePlayingOwner(userId);

            assertThatThrownBy(() ->
                    playingService.getAnalysisContext(userId, playingId)
            ).isInstanceOf(GeneralException.class);

            verify(playing, never()).validateCompleted();
            verify(analysisBarCalculator, never()).calculate(any());
        }
    }

    @Nested
    @DisplayName("연주 기록 삭제")
    class DeletePlaying {

        @Test
        @DisplayName("본인의 연주 기록을 삭제한다")
        void deletePlayingSuccess() {
            // given
            Instant deletedAt =
                    Instant.parse("2026-01-01T15:30:00Z");

            when(playingRepository.findByIdAndDeletedAtIsNull(playingId))
                    .thenReturn(Optional.of(playing));

            when(playing.getId())
                    .thenReturn(playingId);

            doAnswer(invocation -> {
                when(playing.getDeletedAt())
                        .thenReturn(deletedAt);

                return null;
            })
                    .when(playing)
                    .softDelete();

            // when
            PlayingDeleteResponse response =
                    playingService.deletePlaying(userId, playingId);

            // then
            assertThat(response.playingId())
                    .isEqualTo(playingId);

            assertThat(response.deletedAt())
                    .isEqualTo(deletedAt);

            verify(playingRepository)
                    .findByIdAndDeletedAtIsNull(playingId);

            verify(playing)
                    .validatePlayingOwner(userId);

            verify(playing)
                    .softDelete();
        }

        @Test
        @DisplayName("존재하지 않는 연주 기록은 삭제할 수 없다")
        void deletePlayingNotFound() {
            // given
            when(playingRepository.findByIdAndDeletedAtIsNull(playingId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    playingService.deletePlaying(userId, playingId)
            )
                    .isInstanceOf(GeneralException.class)
                    .satisfies(exception -> {
                        GeneralException generalException =
                                (GeneralException) exception;

                        assertThat(generalException.getCode())
                                .isEqualTo(
                                        PlayingErrorStatus.PLAYING_NOT_FOUND
                                );
                    });

            verify(playing, never())
                    .validatePlayingOwner(any());

            verify(playing, never())
                    .softDelete();
        }

        @Test
        @DisplayName("다른 사용자의 연주 기록은 삭제할 수 없다")
        void deletePlayingAccessDenied() {
            // given
            when(playingRepository.findByIdAndDeletedAtIsNull(playingId))
                    .thenReturn(Optional.of(playing));

            doThrow(
                    new GeneralException(
                            PlayingErrorStatus.PLAYING_ACCESS_DENIED
                    )
            )
                    .when(playing)
                    .validatePlayingOwner(userId);

            // when & then
            assertThatThrownBy(() ->
                    playingService.deletePlaying(userId, playingId)
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
                    .softDelete();
        }

        @Test
        @DisplayName("연주 기록 ID가 올바르지 않으면 삭제할 수 없다")
        void deletePlayingInvalidId() {
            // when & then
            assertThatThrownBy(() ->
                    playingService.deletePlaying(userId, 0L)
            )
                    .isInstanceOf(GeneralException.class)
                    .satisfies(exception -> {
                        GeneralException generalException =
                                (GeneralException) exception;

                        assertThat(generalException.getCode())
                                .isEqualTo(
                                        PlayingErrorStatus.INVALID_PLAYING_ID
                                );
                    });

            verify(playingRepository, never())
                    .findByIdAndDeletedAtIsNull(any());
        }
    }

    @Nested
    @DisplayName("녹음 파일 업로드 URL 발급")
    class CreateRecordingUploadUrl {

        @Test
        @DisplayName("진행 중인 본인의 연주이면 녹음 파일 업로드 URL을 발급한다")
        void createRecordingUploadUrl_success() {
            // given
            RecordingUploadUrlRequest request =
                    new RecordingUploadUrlRequest(
                            "recording.mp3",
                            "audio/mpeg",
                            1_024L
                    );

            FileUploadCommand command =
                    request.toCommand();

            PresignedUrlUpload presignedUpload =
                    new PresignedUrlUpload(
                            RECORDING_OBJECT_KEY,
                            "https://example.com/presigned-upload-url",
                            Instant.now().plusSeconds(600),
                            Map.of("Content-Type", "audio/mpeg")
                    );

            RecordingUploadUrlResponse expectedResponse =
                    RecordingUploadUrlResponse.from(
                            presignedUpload
                    );

            when(playingRepository.findByIdAndDeletedAtIsNull(playingId))
                    .thenReturn(Optional.of(playing));

            when(s3FileService.createPresignedUpload(
                    userId,
                    S3FileType.RECORDING,
                    command
            )).thenReturn(presignedUpload);

            // when
            RecordingUploadUrlResponse response =
                    playingService.createRecordingUploadUrl(
                            userId,
                            playingId,
                            request
                    );

            // then
            assertThat(response)
                    .isEqualTo(expectedResponse);

            verify(playingRepository)
                    .findByIdAndDeletedAtIsNull(playingId);

            verify(playing)
                    .validatePlayingOwner(userId);

            verify(playing)
                    .validateInProgress();

            verify(s3FileService)
                    .createPresignedUpload(userId, S3FileType.RECORDING, command);
        }

        @Test
        @DisplayName("진행 중이 아닌 연주에는 녹음 파일 업로드 URL을 발급하지 않는다")
        void createRecordingUploadUrl_notInProgress() {
            // given
            RecordingUploadUrlRequest request =
                    new RecordingUploadUrlRequest(
                            "recording.mp3",
                            "audio/mpeg",
                            1_024L
                    );

            when(playingRepository.findByIdAndDeletedAtIsNull(playingId))
                    .thenReturn(Optional.of(playing));

            doThrow(
                    new GeneralException(
                            PlayingErrorStatus.INVALID_PLAYING_STATUS
                    )
            )
                    .when(playing)
                    .validateInProgress();

            // when & then
            assertThatThrownBy(() ->
                    playingService.createRecordingUploadUrl(
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
                                        PlayingErrorStatus.INVALID_PLAYING_STATUS
                                );
                    });

            verify(playingRepository)
                    .findByIdAndDeletedAtIsNull(playingId);

            verify(playing)
                    .validatePlayingOwner(userId);

            verify(playing)
                    .validateInProgress();

            verify(s3FileService, never())
                    .createPresignedUpload(
                            anyLong(),
                            any(S3FileType.class),
                            any(FileUploadCommand.class)
                    );
        }
    }
}
