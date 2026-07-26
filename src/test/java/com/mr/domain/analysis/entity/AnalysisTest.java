package com.mr.domain.analysis.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.exception.AnalysisErrorStatus;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.user.entity.User;
import com.mr.global.apipayload.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnalysisTest {

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

        assertThatThrownBy(() -> Analysis.createPending(user, playing, 8, 1, "{}"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("createPending - 정상 생성 시 PENDING 상태다")
    void createPending_success_setsPendingStatus() {
        User user = mock(User.class);
        Playing playing = mock(Playing.class);

        Analysis analysis = Analysis.createPending(user, playing, 1, 8, "{}");

        assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.PENDING);
    }
}
