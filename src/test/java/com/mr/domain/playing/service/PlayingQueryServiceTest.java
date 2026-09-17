package com.mr.domain.playing.service;

import com.mr.domain.analysis.service.AnalysisBarCalculator;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.playing.dto.res.AnalysisContextResponse;
import com.mr.domain.playing.dto.res.PlayingDetailResponse;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import com.mr.domain.playing.exception.PlayingErrorStatus;
import com.mr.domain.playing.repository.PlayingRepository;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.file.s3.enums.S3FileType;
import com.mr.global.file.s3.service.S3FileService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayingQueryServiceTest {

    @Mock
    private PlayingRepository playingRepository;

    @Mock
    private AnalysisBarCalculator analysisBarCalculator;

    @Mock
    private S3FileService s3FileService;

    @Mock
    private Playing playing;

    @Mock
    private BackingTrack backingTrack;

    @InjectMocks
    private PlayingQueryService playingQueryService;

    private static final Integer BPM = 120;

    private static final String RECORDING_OBJECT_KEY =
            "recordings/1/2026-08-02/150000_a1b2c3.mp3";

    private static final String RECORDING_FILE_URL =
            "https://example.com/presigned-recording.mp3";

    private Long userId;
    private Long playingId;
    private Long backingTrackId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        playingId = 1L;
        backingTrackId = 1L;
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
            when(playing.getRecordingObjectKey()).thenReturn(RECORDING_OBJECT_KEY);
            when(s3FileService.createPresignedDownload(userId, S3FileType.RECORDING, RECORDING_OBJECT_KEY)).thenReturn(RECORDING_FILE_URL);

            PlayingDetailResponse response =
                    playingQueryService.getPlayingDetail(userId, playingId);

            assertThat(response.playingId()).isEqualTo(playingId);
            assertThat(response.status()).isEqualTo(PlayingStatus.COMPLETED);
            assertThat(response.recordingFileUrl()).isEqualTo(RECORDING_FILE_URL);

            verify(playingRepository).findByIdWithBackingTrack(playingId);
            verify(playing).validatePlayingOwner(userId);
            verify(playing).validateCompleted();
            verify(s3FileService).createPresignedDownload(userId, S3FileType.RECORDING, RECORDING_OBJECT_KEY);
        }

        @Test
        @DisplayName("연주 세션 ID가 1 미만이면 예외가 발생한다")
        void invalidPlayingId() {
            assertThatThrownBy(() ->
                    playingQueryService.getPlayingDetail(1L, 0L)
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
                    playingQueryService.getPlayingDetail(1L, playingId)
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
                    playingQueryService.getPlayingDetail(userId, playingId))
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
                    playingQueryService.getPlayingDetail(userId, playingId))
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
            when(backingTrack.getId()).thenReturn(backingTrackId);
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
                    playingQueryService.getAnalysisContext(userId, playingId);

            assertThat(response.playingId()).isEqualTo(playingId);
            assertThat(response.backingTrackId()).isEqualTo(backingTrackId);
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
                    playingQueryService.getAnalysisContext(userId, playingId)
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
                    playingQueryService.getAnalysisContext(userId, playingId)
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
                    playingQueryService.getAnalysisContext(userId, playingId)
            ).isInstanceOf(GeneralException.class);

            verify(playing, never()).validateCompleted();
            verify(analysisBarCalculator, never()).calculate(any());
        }
    }
}
