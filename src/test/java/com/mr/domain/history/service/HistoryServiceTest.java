package com.mr.domain.history.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisGrade;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.domain.analysis.service.AnalysisBarCalculator;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.history.dto.req.HistoryPeriod;
import com.mr.domain.history.dto.res.HistoryDetailResponseDTO;
import com.mr.domain.history.dto.res.HistoryListResponseDTO;
import com.mr.domain.history.exception.HistoryErrorStatus;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import com.mr.domain.playing.repository.PlayingRepository;
import com.mr.domain.user.entity.User;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.file.s3.service.S3FileService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

    @Mock
    private PlayingRepository playingRepository;

    @Mock
    private AnalysisRepository analysisRepository;

    @Mock
    private S3FileService s3FileService;

    private HistoryService historyService;

    @BeforeEach
    void setUp() {
        historyService = new HistoryService(
                playingRepository, analysisRepository, s3FileService, new AnalysisBarCalculator());
    }

    private Playing mockPlaying(Long playingId, Long userId, PlayingStatus status, LocalDateTime endedAt) {
        User user = mock(User.class);
        lenient().when(user.getUserId()).thenReturn(userId);

        Playing playing = mock(Playing.class);
        lenient().when(playing.getId()).thenReturn(playingId);
        lenient().when(playing.getUser()).thenReturn(user);
        lenient().when(playing.getStatus()).thenReturn(status);
        lenient().when(playing.getEndedAt()).thenReturn(endedAt);
        lenient().when(playing.getDurationSec()).thenReturn(300);
        lenient().when(playing.getBpm()).thenReturn(120);
        lenient().when(playing.getBackingTrack()).thenReturn(null);
        return playing;
    }

    private Analysis completedAnalysis(Long playingId, Integer totalScore) {
        User user = mock(User.class);
        Playing playing = mock(Playing.class);
        lenient().when(playing.getId()).thenReturn(playingId);
        lenient().when(playing.getUser()).thenReturn(user);

        Analysis analysis = Analysis.createPending(user, playing, 1, 8, "{}");
        LocalDateTime now = LocalDateTime.of(2026, 7, 31, 12, 0);
        analysis.startProcessing(now);
        analysis.complete(totalScore, AnalysisGrade.GOOD, "요약", null, null, null, null, null, now);
        return analysis;
    }

    @Test
    @DisplayName("getHistories - 결과가 0건이면 빈 목록을 반환하고 Analysis는 조회하지 않는다")
    void getHistories_empty_returnsEmptyList() {
        given(playingRepository.findPlayingsByUserAndStatus(eq(1L), eq(PlayingStatus.COMPLETED), any()))
                .willReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 10), false));

        HistoryListResponseDTO response = historyService.getHistories(1L, 0, 10, null);

        assertThat(response.items()).isEmpty();
        assertThat(response.hasNext()).isFalse();
        verifyNoInteractions(analysisRepository);
    }

    @Test
    @DisplayName("getHistories - 최신 COMPLETED 분석이 없는 Playing은 latestAnalysisId가 null이다")
    void getHistories_noCompletedAnalysis_latestAnalysisIdIsNull() {
        Playing playing = mockPlaying(1L, 1L, PlayingStatus.COMPLETED, LocalDateTime.now());
        given(playingRepository.findPlayingsByUserAndStatus(eq(1L), eq(PlayingStatus.COMPLETED), any()))
                .willReturn(new SliceImpl<>(List.of(playing), PageRequest.of(0, 10), false));
        given(analysisRepository.findByPlayingIdInAndStatusOrderByCreatedAtDescIdDesc(
                anyList(), eq(AnalysisStatus.COMPLETED)))
                .willReturn(List.of());

        HistoryListResponseDTO response = historyService.getHistories(1L, 0, 10, null);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).latestAnalysisId()).isNull();
        assertThat(response.items().get(0).scoreChange()).isNull();
    }

    @Test
    @DisplayName("getHistories - 마지막 페이지에서는 인접 항목과 점수 차이를 계산하고 마지막 항목은 null이다")
    void getHistories_scoreChange_comparesAdjacentItemsOnLastPage() {
        Playing playing1 = mockPlaying(1L, 1L, PlayingStatus.COMPLETED, LocalDateTime.now());
        Playing playing2 = mockPlaying(2L, 1L, PlayingStatus.COMPLETED, LocalDateTime.now().minusDays(1));
        given(playingRepository.findPlayingsByUserAndStatus(eq(1L), eq(PlayingStatus.COMPLETED), any()))
                .willReturn(new SliceImpl<>(List.of(playing1, playing2), PageRequest.of(0, 10), false));

        Analysis analysis1 = completedAnalysis(1L, 90);
        Analysis analysis2 = completedAnalysis(2L, 80);
        given(analysisRepository.findByPlayingIdInAndStatusOrderByCreatedAtDescIdDesc(
                anyList(), eq(AnalysisStatus.COMPLETED)))
                .willReturn(List.of(analysis1, analysis2));

        HistoryListResponseDTO response = historyService.getHistories(1L, 0, 10, null);

        assertThat(response.items().get(0).scoreChange()).isEqualTo(10);
        assertThat(response.items().get(1).scoreChange()).isNull();
        verify(playingRepository, never()).findNextPlayingId(any(), any(), any(), any(), any());
        verify(playingRepository, never()).findNextPlayingIdSince(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("getHistories - 다음 페이지가 있으면 현재 페이지 마지막 항목도 다음 연주와 점수 차이를 계산한다")
    void getHistories_scoreChange_comparesLastItemWithNextPage() {
        LocalDateTime firstEndedAt = LocalDateTime.of(2026, 7, 31, 12, 0);
        LocalDateTime secondEndedAt = firstEndedAt.minusDays(1);
        Playing playing1 = mockPlaying(1L, 1L, PlayingStatus.COMPLETED, firstEndedAt);
        Playing playing2 = mockPlaying(2L, 1L, PlayingStatus.COMPLETED, secondEndedAt);
        given(playingRepository.findPlayingsByUserAndStatus(
                eq(1L), eq(PlayingStatus.COMPLETED), eq(PageRequest.of(0, 2))))
                .willReturn(new SliceImpl<>(List.of(playing1, playing2), PageRequest.of(0, 2), true));
        given(playingRepository.findNextPlayingId(
                eq(1L), eq(PlayingStatus.COMPLETED), eq(secondEndedAt), eq(2L),
                eq(PageRequest.of(0, 1))))
                .willReturn(List.of(3L));

        Analysis analysis1 = completedAnalysis(1L, 90);
        Analysis analysis2 = completedAnalysis(2L, 80);
        Analysis analysis3 = completedAnalysis(3L, 70);
        given(analysisRepository.findByPlayingIdInAndStatusOrderByCreatedAtDescIdDesc(
                eq(List.of(1L, 2L, 3L)), eq(AnalysisStatus.COMPLETED)))
                .willReturn(List.of(analysis1, analysis2, analysis3));

        HistoryListResponseDTO response = historyService.getHistories(1L, 0, 2, null);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).scoreChange()).isEqualTo(10);
        assertThat(response.items().get(1).scoreChange()).isEqualTo(10);
        assertThat(response.hasNext()).isTrue();
        verify(playingRepository, never()).findNextPlayingIdSince(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("기간 필터가 있으면 cutoff 전용 쿼리로 다음 연주를 조회한다")
    void getHistories_scoreChange_usesSinceQueryWithPeriod() {
        LocalDateTime endedAt = LocalDateTime.of(2026, 7, 31, 12, 0);
        Playing playing = mockPlaying(1L, 1L, PlayingStatus.COMPLETED, endedAt);
        given(playingRepository.findPlayingsByUserAndStatusSince(
                eq(1L), eq(PlayingStatus.COMPLETED), any(LocalDateTime.class),
                eq(PageRequest.of(0, 1))))
                .willReturn(new SliceImpl<>(List.of(playing), PageRequest.of(0, 1), true));
        given(playingRepository.findNextPlayingIdSince(
                eq(1L), eq(PlayingStatus.COMPLETED), any(LocalDateTime.class),
                eq(endedAt), eq(1L), eq(PageRequest.of(0, 1))))
                .willReturn(List.of());
        given(analysisRepository.findByPlayingIdInAndStatusOrderByCreatedAtDescIdDesc(
                eq(List.of(1L)), eq(AnalysisStatus.COMPLETED)))
                .willReturn(List.of());

        historyService.getHistories(1L, 0, 1, HistoryPeriod.WEEKLY);

        verify(playingRepository, never()).findNextPlayingId(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("getHistories - 기간 필터가 있으면 cutoff 전용 쿼리를 사용한다")
    void getHistories_withPeriod_usesSinceQuery() {
        given(playingRepository.findPlayingsByUserAndStatusSince(
                eq(1L), eq(PlayingStatus.COMPLETED), any(LocalDateTime.class), eq(PageRequest.of(0, 10))))
                .willReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 10), false));

        HistoryListResponseDTO response = historyService.getHistories(1L, 0, 10, HistoryPeriod.WEEKLY);

        assertThat(response.items()).isEmpty();
        verify(playingRepository).findPlayingsByUserAndStatusSince(
                eq(1L), eq(PlayingStatus.COMPLETED), any(LocalDateTime.class), eq(PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("getHistories - page가 음수면 400")
    void getHistories_negativePage_throws400() {
        assertThatThrownBy(() -> historyService.getHistories(1L, -1, 10, null))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", HistoryErrorStatus.HISTORY_INVALID_REQUEST);
    }

    @ParameterizedTest
    @DisplayName("getHistories - size가 0 이하거나 51 이상이면 400")
    @ValueSource(ints = {0, 51})
    void getHistories_invalidSize_throws400(int size) {
        assertThatThrownBy(() -> historyService.getHistories(1L, 0, size, null))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", HistoryErrorStatus.HISTORY_INVALID_REQUEST);
    }

    @Test
    @DisplayName("getHistoryDetail - 존재하지 않는 playingId면 404이고 Analysis는 조회하지 않는다")
    void getHistoryDetail_notFound_throws404() {
        given(playingRepository.findByIdWithBackingTrack(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> historyService.getHistoryDetail(1L, 999L))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", HistoryErrorStatus.HISTORY_NOT_FOUND);
        verifyNoInteractions(analysisRepository);
    }

    @ParameterizedTest
    @DisplayName("getHistoryDetail - playingId가 1 미만이면 400이고 조회 자체를 하지 않는다")
    @ValueSource(longs = {0L, -1L})
    void getHistoryDetail_invalidPlayingId_throws400(long playingId) {
        assertThatThrownBy(() -> historyService.getHistoryDetail(1L, playingId))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", HistoryErrorStatus.HISTORY_INVALID_ID);
        verifyNoInteractions(playingRepository);
        verifyNoInteractions(analysisRepository);
    }

    @Test
    @DisplayName("getHistoryDetail - 다른 사용자의 기록이면 403이고 Analysis는 조회하지 않는다")
    void getHistoryDetail_otherUsersPlaying_throws403() {
        Playing playing = mockPlaying(1L, 2L, PlayingStatus.COMPLETED, LocalDateTime.now());
        given(playingRepository.findByIdWithBackingTrack(1L)).willReturn(Optional.of(playing));

        assertThatThrownBy(() -> historyService.getHistoryDetail(1L, 1L))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", HistoryErrorStatus.HISTORY_ACCESS_DENIED);
        verifyNoInteractions(analysisRepository);
    }

    @Test
    @DisplayName("getHistoryDetail - COMPLETED가 아니면 409이고 Analysis는 조회하지 않는다")
    void getHistoryDetail_notCompleted_throws409() {
        Playing playing = mockPlaying(1L, 1L, PlayingStatus.IN_PROGRESS, null);
        given(playingRepository.findByIdWithBackingTrack(1L)).willReturn(Optional.of(playing));

        assertThatThrownBy(() -> historyService.getHistoryDetail(1L, 1L))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", HistoryErrorStatus.HISTORY_NOT_COMPLETED);
        verifyNoInteractions(analysisRepository);
    }

    @Test
    @DisplayName("getHistoryDetail - 정상 조회 시 상태 무관하게 모든 분석을 반환한다")
    void getHistoryDetail_success_returnsAllAnalysesRegardlessOfStatus() {
        Playing playing = mockPlaying(1L, 1L, PlayingStatus.COMPLETED, LocalDateTime.now());
        BackingTrack backingTrack = mock(BackingTrack.class);
        String recordingObjectKey = "recordings/1/2026-08-06/test.webm";
        String recordingFileUrl = "https://example.com/presigned-recording.webm";
        String backingTrackAudioFileUrl = backingTrack.toString();
        given(playing.getRecordingObjectKey()).willReturn(recordingObjectKey);
        given(s3FileService.createPresignedDownload(1L, recordingObjectKey)).willReturn(recordingFileUrl);
        given(playing.getBackingTrack()).willReturn(backingTrack);
        given(backingTrack.getAudioFileUrl()).willReturn(backingTrackAudioFileUrl);
        given(backingTrack.getTimeSignature()).willReturn("4/4");
        given(backingTrack.getPlaytimeSec()).willReturn(120);
        given(playingRepository.findByIdWithBackingTrack(1L)).willReturn(Optional.of(playing));

        Analysis completed = completedAnalysis(1L, 90);
        User pendingUser = mock(User.class);
        Playing pendingPlaying = mock(Playing.class);
        given(pendingPlaying.getUser()).willReturn(pendingUser);
        Analysis pending = Analysis.createPending(pendingUser, pendingPlaying, 9, 16, "{}");
        given(analysisRepository.findByPlayingIdAndUserIdOrderByStartBarAscIdAsc(1L, 1L))
                .willReturn(List.of(completed, pending));

        HistoryDetailResponseDTO response = historyService.getHistoryDetail(1L, 1L);

        verify(s3FileService).createPresignedDownload(1L, recordingObjectKey);

        assertThat(response.recordingFileUrl()).isEqualTo(recordingFileUrl);
        assertThat(response.backingTrackAudioFileUrl()).isEqualTo(backingTrackAudioFileUrl);
        assertThat(response.analyses()).hasSize(2);
        assertThat(response.analyses().get(1).status()).isEqualTo(AnalysisStatus.PENDING);
    }

    @Test
    @DisplayName("getHistoryDetail - BPM/박자/재생시간이 있으면 totalBars와 구간별 estimatedSeconds를 계산한다")
    void getHistoryDetail_success_calculatesBarMetrics() {
        Playing playing = mockPlaying(1L, 1L, PlayingStatus.COMPLETED, LocalDateTime.now());
        BackingTrack backingTrack = mock(BackingTrack.class);
        given(playing.getBackingTrack()).willReturn(backingTrack);
        given(backingTrack.getTimeSignature()).willReturn("4/4");
        given(backingTrack.getPlaytimeSec()).willReturn(120);
        given(playingRepository.findByIdWithBackingTrack(1L)).willReturn(Optional.of(playing));

        Analysis completed = completedAnalysis(1L, 90);
        given(analysisRepository.findByPlayingIdAndUserIdOrderByStartBarAscIdAsc(1L, 1L))
                .willReturn(List.of(completed));

        HistoryDetailResponseDTO response = historyService.getHistoryDetail(1L, 1L);

        // BPM 120, 4/4 -> 마디당 2초, 재생 120초 -> 60마디 / 1~8마디 분석 -> 16초
        assertThat(response.totalBars()).isEqualTo(60);
        assertThat(response.analyses().get(0).estimatedSeconds()).isEqualTo(16);
    }

    @Test
    @DisplayName("getHistoryDetail - 마디 계산 정보가 없으면 조회는 성공하고 마디 관련 필드만 null이다")
    void getHistoryDetail_missingBarMetrics_returnsNullBarFields() {
        Playing playing = mockPlaying(1L, 1L, PlayingStatus.COMPLETED, LocalDateTime.now());
        BackingTrack backingTrack = mock(BackingTrack.class);
        given(playing.getBackingTrack()).willReturn(backingTrack);
        given(backingTrack.getPlaytimeSec()).willReturn(null);
        given(playingRepository.findByIdWithBackingTrack(1L)).willReturn(Optional.of(playing));

        Analysis completed = completedAnalysis(1L, 90);
        given(analysisRepository.findByPlayingIdAndUserIdOrderByStartBarAscIdAsc(1L, 1L))
                .willReturn(List.of(completed));

        HistoryDetailResponseDTO response = historyService.getHistoryDetail(1L, 1L);

        assertThat(response.totalBars()).isNull();
        assertThat(response.analyses().get(0).estimatedSeconds()).isNull();
    }
}
