package com.mr.domain.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.mr.domain.analysis.exception.AnalysisErrorStatus;
import com.mr.domain.backingTrack.entity.BackingTrack;
import com.mr.domain.backingTrack.entity.enums.Level;
import com.mr.domain.backingTrack.entity.enums.ScaleType;
import com.mr.domain.playing.entity.MidiEventData;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.MidiType;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.client.ai.AiAnalysisRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnalysisRequestFactoryTest {

    private AnalysisRequestFactory factory;
    private Playing playing;
    private BackingTrack track;

    @BeforeEach
    void setUp() {
        factory = new AnalysisRequestFactory();
        playing = mock(Playing.class);
        track = mock(BackingTrack.class);
        given(playing.getBackingTrack()).willReturn(track);
        given(playing.getBpm()).willReturn(120);
        given(track.getTimeSignature()).willReturn("4/4");
        given(track.getPlaytimeSec()).willReturn(120);
        given(track.getChordProgressions()).willReturn(List.of());
        given(track.getKeySignature()).willReturn("C");
        given(track.getScaleType()).willReturn(ScaleType.MAJOR);
        given(track.getGenre()).willReturn("JAZZ");
        given(track.getLevel()).willReturn(Level.BASIC);
    }

    @Test
    void create_slicesAndRebasesMidiByBar() {
        given(playing.getMidiData()).willReturn(List.of(
                MidiEventData.of(0, MidiType.NOTE_ON, 60, 100, 0L),
                MidiEventData.of(1, MidiType.NOTE_ON, 62, 100, 2_000L),
                MidiEventData.of(2, MidiType.NOTE_OFF, 62, 0, 3_999L),
                MidiEventData.of(3, MidiType.NOTE_ON, 64, 100, 4_000L)
        ));

        AiAnalysisRequest request = factory.create(playing, 2, 2);

        assertThat(request.notes()).hasSize(2);
        assertThat(request.notes().get(0).timestampMs()).isEqualTo(0D);
        assertThat(request.notes().get(1).timestampMs()).isEqualTo(1_999D);
    }

    @Test
    void create_rejectsMoreThanThirtyTwoBars() {
        assertThatThrownBy(() -> factory.create(playing, 1, 33))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", AnalysisErrorStatus.INVALID_BAR_RANGE);
    }
}
