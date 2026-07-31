package com.mr.domain.backingtrack.service;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.exception.AnalysisErrorStatus;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.domain.backingtrack.dto.req.BackingTrackListRequestDTO;
import com.mr.domain.backingtrack.dto.req.PlayCountIncreaseRequestDTO;
import com.mr.domain.backingtrack.dto.res.BackingTrackListResponseDTO;
import com.mr.domain.backingtrack.dto.res.PlayCountIncreaseResponseDTO;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.backingtrack.entity.enums.AccessLevel;
import com.mr.domain.backingtrack.exception.BackingTrackErrorStatus;
import com.mr.domain.backingtrack.repository.BackingTrackRepository;
import com.mr.domain.playing.entity.Playing;
import com.mr.global.apipayload.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackingTrackServiceTest {

    @Mock
    private BackingTrackRepository backingTrackRepository;

    @Mock
    private AnalysisRepository analysisRepository;

    @InjectMocks
    private BackingTrackService backingTrackService;

    @Mock
    private BackingTrack backingTrack;

    @Mock
    private Analysis analysis;

    private Long userId;
    private Long backingTrackId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        backingTrackId = 100L;
    }

    @Nested
    @DisplayName("재생수 증가")
    class IncreasePlayCount {

        @Test
        @DisplayName("본인 분석 결과이고 트랙이 일치하면 재생수가 증가한다")
        void increasePlayCount_success() {
            // given
            Long analysisId = 200L;
            // NOTE: PlayCountIncreaseRequestDTO.IncreaseRequestDTO 실제 필드명이
            // analysisId()가 아니라면 이 부분을 실제 record 정의에 맞게 바꿔줘야 함
            PlayCountIncreaseRequestDTO.IncreaseRequestDTO request =
                    new PlayCountIncreaseRequestDTO.IncreaseRequestDTO(analysisId);

            Playing playing = mock(Playing.class);
            BackingTrack playingTrack = mock(BackingTrack.class);

            given(backingTrackRepository.findByIdAndDeletedAtIsNull(backingTrackId))
                    .willReturn(Optional.of(backingTrack));

            given(analysisRepository.findById(analysisId))
                    .willReturn(Optional.of(analysis));

            given(analysis.getStatus()).willReturn(AnalysisStatus.COMPLETED);
            given(analysis.getPlaying()).willReturn(playing);
            given(playing.getBackingTrack()).willReturn(playingTrack);
            given(playingTrack.getId()).willReturn(backingTrackId);

            given(backingTrackRepository.increasePlayCount(backingTrackId))
                    .willReturn(1);

            given(backingTrackRepository.findByIdAndDeletedAtIsNull(backingTrackId))
                    .willReturn(Optional.of(backingTrack));
            given(backingTrack.getId()).willReturn(backingTrackId);
            given(backingTrack.getPlayCount()).willReturn(6);

            // when
            PlayCountIncreaseResponseDTO.IncreaseResponseDTO response =
                    backingTrackService.increasePlayCount(backingTrackId, request, userId);

            // then
            assertThat(response.playCount()).isEqualTo(6);
            verify(analysis).validateOwner(userId);
        }

        @Test
        @DisplayName("완료되지 않은 분석이면 예외가 발생한다")
        void increasePlayCount_notCompleted() {
            Long analysisId = 200L;
            PlayCountIncreaseRequestDTO.IncreaseRequestDTO request =
                    new PlayCountIncreaseRequestDTO.IncreaseRequestDTO(analysisId);

            given(backingTrackRepository.findByIdAndDeletedAtIsNull(backingTrackId))
                    .willReturn(Optional.of(backingTrack));

            given(analysisRepository.findById(analysisId))
                    .willReturn(Optional.of(analysis));

            given(analysis.getStatus()).willReturn(AnalysisStatus.PROCESSING);

            assertThatThrownBy(() ->
                    backingTrackService.increasePlayCount(backingTrackId, request, userId)
            )
                    .isInstanceOf(GeneralException.class)
                    .satisfies(exception -> {
                        GeneralException generalException = (GeneralException) exception;
                        assertThat(generalException.getCode())
                                .isEqualTo(AnalysisErrorStatus.ANALYSIS_NOT_COMPLETED);
                    });

            verify(backingTrackRepository, never()).increasePlayCount(anyLong());
        }

        @Test
        @DisplayName("본인 분석 결과가 아니면 재생수를 증가시키지 않는다")
        void increasePlayCount_notOwner() {
            Long analysisId = 200L;
            PlayCountIncreaseRequestDTO.IncreaseRequestDTO request =
                    new PlayCountIncreaseRequestDTO.IncreaseRequestDTO(analysisId);

            given(backingTrackRepository.findByIdAndDeletedAtIsNull(backingTrackId))
                    .willReturn(Optional.of(backingTrack));

            given(analysisRepository.findById(analysisId))
                    .willReturn(Optional.of(analysis));

            given(analysis.getStatus()).willReturn(AnalysisStatus.COMPLETED);

            org.mockito.Mockito.doThrow(
                    new GeneralException(AnalysisErrorStatus.ANALYSIS_ACCESS_DENIED)
            ).when(analysis).validateOwner(userId);

            assertThatThrownBy(() ->
                    backingTrackService.increasePlayCount(backingTrackId, request, userId)
            )
                    .isInstanceOf(GeneralException.class)
                    .satisfies(exception -> {
                        GeneralException generalException = (GeneralException) exception;
                        assertThat(generalException.getCode())
                                .isEqualTo(AnalysisErrorStatus.ANALYSIS_ACCESS_DENIED);
                    });

            verify(backingTrackRepository, never()).increasePlayCount(anyLong());
        }

        @Test
        @DisplayName("분석 결과의 트랙과 요청 트랙이 다르면 예외가 발생한다")
        void increasePlayCount_trackMismatch() {
            Long analysisId = 200L;
            PlayCountIncreaseRequestDTO.IncreaseRequestDTO request =
                    new PlayCountIncreaseRequestDTO.IncreaseRequestDTO(analysisId);

            Playing playing = mock(Playing.class);
            BackingTrack otherTrack = mock(BackingTrack.class);

            given(backingTrackRepository.findByIdAndDeletedAtIsNull(backingTrackId))
                    .willReturn(Optional.of(backingTrack));

            given(analysisRepository.findById(analysisId))
                    .willReturn(Optional.of(analysis));

            given(analysis.getStatus()).willReturn(AnalysisStatus.COMPLETED);
            given(analysis.getPlaying()).willReturn(playing);
            given(playing.getBackingTrack()).willReturn(otherTrack);
            given(otherTrack.getId()).willReturn(999L); // 다른 트랙 id

            assertThatThrownBy(() ->
                    backingTrackService.increasePlayCount(backingTrackId, request, userId)
            )
                    .isInstanceOf(GeneralException.class)
                    .satisfies(exception -> {
                        GeneralException generalException = (GeneralException) exception;
                        assertThat(generalException.getCode())
                                .isEqualTo(BackingTrackErrorStatus.ANALYSIS_TRACK_MISMATCH);
                    });

            verify(backingTrackRepository, never()).increasePlayCount(anyLong());
        }

        @Test
        @DisplayName("존재하지 않는 백킹트랙이면 예외가 발생한다")
        void increasePlayCount_trackNotFound() {
            Long analysisId = 200L;
            PlayCountIncreaseRequestDTO.IncreaseRequestDTO request =
                    new PlayCountIncreaseRequestDTO.IncreaseRequestDTO(analysisId);

            given(backingTrackRepository.findByIdAndDeletedAtIsNull(backingTrackId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    backingTrackService.increasePlayCount(backingTrackId, request, userId)
            )
                    .isInstanceOf(GeneralException.class)
                    .satisfies(exception -> {
                        GeneralException generalException = (GeneralException) exception;
                        assertThat(generalException.getCode())
                                .isEqualTo(BackingTrackErrorStatus.BACKING_TRACK_NOT_FOUND);
                    });

            verify(analysisRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("목록 조회 (커서 기반)")
    class GetBackingTracks {

        @Test
        @DisplayName("결과가 PAGE_SIZE보다 많으면 hasNext가 true이고 마지막 항목이 잘린다")
        void getBackingTracks_hasNextTrue() {
            // given: PAGE_SIZE(9) + 1 = 10개를 리포지토리가 반환하는 상황 가정
            // NOTE: PAGE_SIZE는 BackingTrackService 내부 private 상수(9)이므로
            // 실제 값에 맞춰 mock 데이터 개수를 조정해야 함
            BackingTrackListRequestDTO.ListRequestDTO request =
                    new BackingTrackListRequestDTO.ListRequestDTO(null);

            List<BackingTrack> tenTracks = createMockTracks(10);

            given(backingTrackRepository.findVisibleTracksAfterCursor(
                    eq(AccessLevel.PUBLIC), eq(userId), isNull(), any()
            )).willReturn(tenTracks);

            // when
            BackingTrackListResponseDTO.ListResponseDTO response =
                    backingTrackService.getBackingTracks(request, userId);

            // then
            assertThat(response.hasNext()).isTrue();
            assertThat(response.tracks()).hasSize(9);
            assertThat(response.nextCursor()).isEqualTo(tenTracks.get(8).getId());
        }

        @Test
        @DisplayName("결과가 PAGE_SIZE 이하이면 hasNext가 false이다")
        void getBackingTracks_hasNextFalse() {
            BackingTrackListRequestDTO.ListRequestDTO request =
                    new BackingTrackListRequestDTO.ListRequestDTO(null);

            List<BackingTrack> fiveTracks = createMockTracks(5);

            given(backingTrackRepository.findVisibleTracksAfterCursor(
                    eq(AccessLevel.PUBLIC), eq(userId), isNull(), any()
            )).willReturn(fiveTracks);

            BackingTrackListResponseDTO.ListResponseDTO response =
                    backingTrackService.getBackingTracks(request, userId);

            assertThat(response.hasNext()).isFalse();
            assertThat(response.tracks()).hasSize(5);
            assertThat(response.nextCursor()).isEqualTo(fiveTracks.get(4).getId());
        }

        @Test
        @DisplayName("결과가 없으면 nextCursor는 null이다")
        void getBackingTracks_empty() {
            BackingTrackListRequestDTO.ListRequestDTO request =
                    new BackingTrackListRequestDTO.ListRequestDTO(null);

            given(backingTrackRepository.findVisibleTracksAfterCursor(
                    eq(AccessLevel.PUBLIC), eq(userId), isNull(), any()
            )).willReturn(Collections.emptyList());

            BackingTrackListResponseDTO.ListResponseDTO response =
                    backingTrackService.getBackingTracks(request, userId);

            assertThat(response.hasNext()).isFalse();
            assertThat(response.tracks()).isEmpty();
            assertThat(response.nextCursor()).isNull();
        }

        // NOTE: BackingTrack이 mock이라 getChordProgressions()가 기본적으로 빈 리스트를
        // 반환해야 서비스 내 stream 처리가 NPE 없이 동작함. Mockito는 컬렉션 반환 타입에
        // 기본적으로 emptyList를 리턴하므로 별도 stubbing 없이도 동작할 것으로 예상되나,
        // 실패 시 given(track.getChordProgressions()).willReturn(List.of())를 추가해야 함
        private List<BackingTrack> createMockTracks(int count) {
            List<BackingTrack> tracks = new java.util.ArrayList<>();
            for (long i = 1; i <= count; i++) {
                BackingTrack track = mock(BackingTrack.class);
                lenient().when(track.getId()).thenReturn(i);
                lenient().when(track.getScaleType())
                        .thenReturn(com.mr.domain.backingtrack.entity.enums.ScaleType.MAJOR);
                lenient().when(track.getLevel())
                        .thenReturn(com.mr.domain.backingtrack.entity.enums.Level.BASIC);
                lenient().when(track.getChordProgressions()).thenReturn(List.of());
                tracks.add(track);
            }
            return tracks;
        }
    }
}