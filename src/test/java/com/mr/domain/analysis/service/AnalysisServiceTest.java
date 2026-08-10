package com.mr.domain.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.analysis.dto.req.AnalysisCreateRequestDTO;
import com.mr.domain.analysis.dto.res.AnalysisCreateResponseDTO;
import com.mr.domain.analysis.dto.res.AnalysisResultResponseDTO;
import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.AnalysisReport;
import com.mr.domain.analysis.entity.enums.AnalysisGrade;
import com.mr.domain.analysis.entity.enums.LlmStatus;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.event.AnalysisRequestedEvent;
import com.mr.domain.analysis.exception.AnalysisErrorStatus;
import com.mr.domain.analysis.factory.AnalysisRequestFactory;
import com.mr.domain.analysis.repository.AnalysisReportRepository;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.backingtrack.entity.enums.ScaleType;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import com.mr.domain.playing.repository.PlayingRepository;
import com.mr.domain.user.entity.User;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.file.s3.service.S3FileService;
import com.mr.global.client.ai.AiAnalysisRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    private static final String RECORDING_OBJECT_KEY = "recordings/1/2026-08-06/120000_abcdef.webm";
    private static final String RECORDING_FILE_URL = "https://example.com/presigned-recording.webm";

    @Mock
    private AnalysisRepository analysisRepository;

    @Mock
    private AnalysisReportRepository analysisReportRepository;

    @Mock
    private PlayingRepository playingRepository;

    @Mock
    private AnalysisRequestFactory analysisRequestFactory;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private S3FileService s3FileService;

    private AnalysisService analysisService;

    @BeforeEach
    void setUp() {
        analysisService = new AnalysisService(analysisRepository, analysisReportRepository, playingRepository,
                analysisRequestFactory, eventPublisher, new ObjectMapper(), s3FileService);
    }

    private Analysis completedAnalysis(Long userId) {
        return completedAnalysis(userId, ScaleType.MAJOR);
    }

    private Analysis completedAnalysis(Long userId, ScaleType scaleType) {
        User user = mock(User.class);
        given(user.getUserId()).willReturn(userId);

        Playing playing = mock(Playing.class);
        BackingTrack backingTrack = mock(BackingTrack.class);
        lenient().when(playing.getId()).thenReturn(1L);
        lenient().when(playing.getUser()).thenReturn(user);
        lenient().when(playing.getBackingTrack()).thenReturn(backingTrack);
        lenient().when(playing.getBpm()).thenReturn(120);
        lenient().when(playing.getRecordingObjectKey()).thenReturn(RECORDING_OBJECT_KEY);
        lenient().when(backingTrack.getTitle()).thenReturn("테스트 트랙");
        lenient().when(backingTrack.getGenre()).thenReturn("jazz");
        lenient().when(backingTrack.getKeySignature()).thenReturn("C");
        lenient().when(backingTrack.getScaleType()).thenReturn(scaleType);
        lenient().when(backingTrack.getAudioFileUrl()).thenReturn("https://example.com/backing-track.mp3");

        Analysis analysis = Analysis.createPending(user, playing, 1, 8, "{}");
        LocalDateTime now = LocalDateTime.of(2026, 7, 31, 12, 0);
        analysis.startProcessing(now);
        analysis.complete(
                85,
                AnalysisGrade.GOOD,
                "테스트 요약",
                new BigDecimal("80.00"),
                new BigDecimal("75.50"),
                new BigDecimal("90.00"),
                new BigDecimal("70.00"),
                null,
                now
        );
        return analysis;
    }

    @Test
    @DisplayName("getAnalysisResult - JVM 로케일과 무관하게 조성명을 변환한다")
    void getAnalysisResult_turkishLocale_formatsKeyConsistently() {
        Locale previousLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            Analysis analysis = completedAnalysis(1L, ScaleType.MINOR);
            given(analysisRepository.findById(1L)).willReturn(Optional.of(analysis));
            given(analysisReportRepository.findFirstByAnalysisIdAndLlmStatusOrderByCreatedAtDesc(anyLong(), any()))
                    .willReturn(Optional.empty());

            AnalysisResultResponseDTO response = analysisService.getAnalysisResult(1L, 1L);

            assertThat(response.key()).isEqualTo("C Minor");
        } finally {
            Locale.setDefault(previousLocale);
        }
    }

    @Test
    @DisplayName("getAnalysisResult - SUCCESS 리포트가 있으면 report를 채워서 반환한다")
    void getAnalysisResult_success_returnsLatestSuccessReport() {
        Long analysisId = 1L;
        Long userId = 1L;
        Analysis analysis = completedAnalysis(userId);
        AnalysisReport report = AnalysisReport.createLlmReport(analysis, "리포트 본문", "gpt-4", "v1");

        given(analysisRepository.findById(analysisId)).willReturn(Optional.of(analysis));
        given(analysisReportRepository.findFirstByAnalysisIdAndLlmStatusOrderByCreatedAtDesc(anyLong(), any()))
                .willReturn(Optional.of(report));
        given(s3FileService.createPresignedDownload(userId, RECORDING_OBJECT_KEY)).willReturn(RECORDING_FILE_URL);

        AnalysisResultResponseDTO response = analysisService.getAnalysisResult(userId, analysisId);

        ArgumentCaptor<LlmStatus> llmStatusCaptor = ArgumentCaptor.forClass(LlmStatus.class);
        verify(analysisReportRepository)
                .findFirstByAnalysisIdAndLlmStatusOrderByCreatedAtDesc(eq(analysisId), llmStatusCaptor.capture());
        verify(s3FileService).createPresignedDownload(userId, RECORDING_OBJECT_KEY);
        assertThat(llmStatusCaptor.getValue()).isEqualTo(LlmStatus.SUCCESS);

        assertThat(response.report()).isNotNull();
        assertThat(response.report().content()).isEqualTo("리포트 본문");
        assertThat(response.report().llmStatus()).isEqualTo(LlmStatus.SUCCESS);
        assertThat(response.title()).isEqualTo("테스트 트랙");
        assertThat(response.key()).isEqualTo("C Major");
        assertThat(response.recordingFileUrl()).isEqualTo(RECORDING_FILE_URL);
        assertThat(response.backingTrackAudioFileUrl()).isEqualTo("https://example.com/backing-track.mp3");
    }

    @Test
    @DisplayName("getAnalysisResult - 백킹트랙이 없으면 음원 URL은 null이다")
    void getAnalysisResult_withoutBackingTrack_returnsNullAudioUrls() {
        User user = mock(User.class);
        Playing playing = mock(Playing.class);
        given(user.getUserId()).willReturn(1L);
        given(playing.getUser()).willReturn(user);
        given(playing.getId()).willReturn(1L);

        Analysis analysis = Analysis.createPending(user, playing, 1, 8, "{}");
        LocalDateTime now = LocalDateTime.of(2026, 7, 31, 12, 0);
        analysis.startProcessing(now);
        analysis.complete(85, AnalysisGrade.GOOD, "테스트 요약", null, null, null, null, null, now);
        given(analysisRepository.findById(1L)).willReturn(Optional.of(analysis));
        given(analysisReportRepository.findFirstByAnalysisIdAndLlmStatusOrderByCreatedAtDesc(anyLong(), any()))
                .willReturn(Optional.empty());

        AnalysisResultResponseDTO response = analysisService.getAnalysisResult(1L, 1L);

        assertThat(response.recordingFileUrl()).isNull();
        assertThat(response.backingTrackAudioFileUrl()).isNull();
    }

    @Test
    @DisplayName("getAnalysisResult - SUCCESS 리포트가 없으면 report는 null이지만 나머지 필드는 정상 반환한다")
    void getAnalysisResult_noSuccessReport_returnsNullReport() {
        Long analysisId = 1L;
        Long userId = 1L;
        Analysis analysis = completedAnalysis(userId);

        given(analysisRepository.findById(analysisId)).willReturn(Optional.of(analysis));
        given(analysisReportRepository.findFirstByAnalysisIdAndLlmStatusOrderByCreatedAtDesc(anyLong(), any()))
                .willReturn(Optional.empty());

        AnalysisResultResponseDTO response = analysisService.getAnalysisResult(userId, analysisId);

        assertThat(response.report()).isNull();
        assertThat(response.totalScore()).isEqualTo(85);
        assertThat(response.grade()).isEqualTo(AnalysisGrade.GOOD);
        assertThat(response.domainScores().scaleScore()).isEqualByComparingTo(new BigDecimal("80.00"));
    }

    @Test
    @DisplayName("getAnalysisResult - 다른 사용자의 분석이면 접근이 거부된다")
    void getAnalysisResult_otherUsersAnalysis_throwsAccessDenied() {
        Long analysisId = 1L;
        Analysis analysis = completedAnalysis(1L);

        given(analysisRepository.findById(analysisId)).willReturn(Optional.of(analysis));

        assertThatThrownBy(() -> analysisService.getAnalysisResult(2L, analysisId))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", AnalysisErrorStatus.ANALYSIS_ACCESS_DENIED);
    }
    @Test
    @DisplayName("createAnalysis - saves a pending analysis and publishes an event")
    void createAnalysis_success() {
        User user = mock(User.class);
        Playing playing = mock(Playing.class);
        given(user.getUserId()).willReturn(1L);
        given(playing.getId()).willReturn(31L);
        given(playing.getUser()).willReturn(user);
        given(playing.getStatus()).willReturn(PlayingStatus.COMPLETED);
        given(playingRepository.findByIdWithBackingTrackForUpdate(31L)).willReturn(Optional.of(playing));
        given(analysisRequestFactory.create(playing, 1, 8))
                .willReturn(new AiAnalysisRequest(null, List.of(), List.of()));
        given(analysisRepository.save(any(Analysis.class)))
                .willAnswer((Answer<Analysis>) invocation -> invocation.getArgument(0));

        AnalysisCreateResponseDTO response = analysisService.createAnalysis(
                1L, new AnalysisCreateRequestDTO(31L, 1, 8)
        );

        assertThat(response.playingId()).isEqualTo(31L);
        assertThat(response.status()).isEqualTo(AnalysisStatus.PENDING);
        verify(eventPublisher).publishEvent(any(AnalysisRequestedEvent.class));
    }


}
