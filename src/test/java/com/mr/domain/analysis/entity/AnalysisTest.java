package com.mr.domain.analysis.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.exception.AnalysisErrorStatus;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.user.entity.User;
import com.mr.global.apipayload.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

class AnalysisTest {

    private static final Instant NOW = Instant.parse("2026-07-31T12:00:00Z");

    @Test
    @DisplayName("createPending - user가 null이면 예외가 발생한다")
    void createPending_userNull_throwsException() {
        Playing playing = mock(Playing.class);

        assertThatThrownBy(() -> Analysis.createPending(null, playing, 1, 8, "{}"))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", AnalysisErrorStatus.ANALYSIS_INVALID_REQUEST);
    }

    @Test
    @DisplayName("createPending - playing이 null이면 예외가 발생한다")
    void createPending_playingNull_throwsException() {
        User user = mock(User.class);

        assertThatThrownBy(() -> Analysis.createPending(user, null, 1, 8, "{}"))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", AnalysisErrorStatus.ANALYSIS_INVALID_REQUEST);
    }

    @Test
    @DisplayName("createPending - startBar가 endBar보다 크면 예외가 발생한다")
    void createPending_invalidBarRange_throwsException() {
        User user = mock(User.class);
        Playing playing = mock(Playing.class);
        given(playing.getUser()).willReturn(user);

        assertThatThrownBy(() -> Analysis.createPending(user, playing, 8, 1, "{}"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("createPending - 정상 생성 시 PENDING 상태다")
    void createPending_success_setsPendingStatus() {
        User user = mock(User.class);
        Playing playing = mock(Playing.class);
        given(playing.getUser()).willReturn(user);

        Analysis analysis = Analysis.createPending(user, playing, 1, 8, "{}");

        assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.PENDING);
    }

    @Test
    @DisplayName("startProcessing - 처리 시작 시각을 기록한다")
    void startProcessing_recordsProcessingStartedAt() {
        User user = mock(User.class);
        Playing playing = mock(Playing.class);
        given(playing.getUser()).willReturn(user);
        Analysis analysis = Analysis.createPending(user, playing, 1, 8, "{}");

        analysis.startProcessing(NOW);

        assertThat(analysis.getProcessingStartedAt()).isEqualTo(NOW);
        assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.PROCESSING);
    }

    @Test
    @DisplayName("restartProcessing - 새 처리 시작 시각으로 이전 작업을 펜싱한다")
    void restartProcessing_renewsProcessingStartedAt() {
        User user = mock(User.class);
        Playing playing = mock(Playing.class);
        given(playing.getUser()).willReturn(user);
        Analysis analysis = Analysis.createPending(user, playing, 1, 8, "{}");
        Instant firstAttempt = analysis.startProcessing(NOW);

        Instant secondAttempt = analysis.restartProcessing(NOW);

        assertThat(secondAttempt).isAfter(firstAttempt);
        assertThat(analysis.isCurrentProcessing(firstAttempt)).isFalse();
        assertThat(analysis.isCurrentProcessing(secondAttempt)).isTrue();
    }

    @Test
    @DisplayName("createPending - playing 소유자와 user가 다르면 예외가 발생한다")
    void createPending_ownerMismatch_throwsException() {
        User user = mock(User.class);
        given(user.getUserId()).willReturn(1L);
        User otherUser = mock(User.class);
        given(otherUser.getUserId()).willReturn(2L);
        Playing playing = mock(Playing.class);
        given(playing.getUser()).willReturn(otherUser);

        assertThatThrownBy(() -> Analysis.createPending(user, playing, 1, 8, "{}"))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", AnalysisErrorStatus.ANALYSIS_OWNER_MISMATCH);
    }
}
