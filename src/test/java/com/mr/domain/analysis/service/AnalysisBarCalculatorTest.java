package com.mr.domain.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.mr.domain.analysis.exception.AnalysisErrorStatus;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.playing.entity.Playing;
import com.mr.global.apipayload.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnalysisBarCalculatorTest {

    private AnalysisBarCalculator calculator;
    private Playing playing;
    private BackingTrack track;

    @BeforeEach
    void setUp() {
        calculator = new AnalysisBarCalculator();
        playing = mock(Playing.class);
        track = mock(BackingTrack.class);
        given(playing.getBackingTrack()).willReturn(track);
    }

    @Test
    void calculate_usesSameCeilingRuleAsAnalysisRangeValidation() {
        given(playing.getBpm()).willReturn(120);
        given(track.getTimeSignature()).willReturn("4/4");
        given(track.getPlaytimeSec()).willReturn(121);

        AnalysisBarCalculator.BarMetrics result = calculator.calculate(playing);

        assertThat(result.barDurationMs()).isEqualTo(2_000D);
        assertThat(result.totalBars()).isEqualTo(61);
    }

    @Test
    void calculate_rejectsInvalidTimeSignature() {
        given(playing.getBpm()).willReturn(120);
        given(track.getTimeSignature()).willReturn("invalid");
        given(track.getPlaytimeSec()).willReturn(120);

        assertThatThrownBy(() -> calculator.calculate(playing))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue(
                        "code",
                        AnalysisErrorStatus.ANALYSIS_INVALID_REQUEST
                );
    }
}
