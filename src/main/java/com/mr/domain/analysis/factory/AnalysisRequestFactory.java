package com.mr.domain.analysis.factory;

import com.mr.domain.analysis.exception.AnalysisErrorStatus;
import com.mr.domain.analysis.service.AnalysisBarCalculator;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.backingtrack.entity.ChordProgression;
import com.mr.domain.playing.entity.MidiEventData;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.MidiType;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.client.ai.AiAnalysisRequest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalysisRequestFactory {

    private static final int MAX_BAR_COUNT = 32;
    private final AnalysisBarCalculator barCalculator;

    public AiAnalysisRequest create(Playing playing, int startBar, int endBar) {
        BackingTrack track = playing.getBackingTrack();
        AnalysisBarCalculator.BarMetrics barMetrics = barCalculator.calculate(playing);
        int[] timeSignature = barMetrics.timeSignature();
        double barDurationMs = barMetrics.barDurationMs();

        validateBarRange(startBar, endBar, barMetrics.totalBars());

        double startOffsetMs = (startBar - 1) * barDurationMs;
        double endOffsetMs = endBar * barDurationMs;
        AtomicInteger noteIndex = new AtomicInteger();

        List<AiAnalysisRequest.Note> notes = playing.getMidiData().stream()
                .filter(event -> event.getTimestampMs() >= startOffsetMs
                        && event.getTimestampMs() < endOffsetMs)
                .sorted(Comparator.comparingLong(MidiEventData::getTimestampMs)
                        .thenComparingInt(MidiEventData::getSequence))
                .map(event -> new AiAnalysisRequest.Note(
                        noteIndex.getAndIncrement(),
                        toNoteType(event.getType()),
                        event.getPitch(),
                        event.getVelocity(),
                        event.getTimestampMs() - startOffsetMs
                ))
                .toList();
        if (notes.isEmpty()) {
            throw new GeneralException(AnalysisErrorStatus.EMPTY_NOTE_RANGE);
        }

        List<AiAnalysisRequest.Chord> chords = track.getChordProgressions().stream()
                .filter(chord -> chord.getMeasureNo() >= startBar && chord.getMeasureNo() <= endBar)
                .sorted(Comparator.comparingInt(ChordProgression::getMeasureNo)
                        .thenComparingInt(ChordProgression::getSequenceNo))
                .map(chord -> new AiAnalysisRequest.Chord(
                        chord.getMeasureNo() - startBar + 1,
                        chord.getSequenceNo().doubleValue(),
                        chord.getChordName()
                ))
                .toList();

        AiAnalysisRequest.Meta meta = new AiAnalysisRequest.Meta(
                playing.getBpm().doubleValue(),
                Arrays.stream(timeSignature).boxed().toList(),
                new AiAnalysisRequest.Key(
                        track.getKeySignature(),
                        track.getScaleType().name().toLowerCase(Locale.ROOT)
                ),
                track.getGenre().toLowerCase(Locale.ROOT),
                track.getLevel().name().toLowerCase(Locale.ROOT)
        );
        return new AiAnalysisRequest(meta, chords, notes);
    }

    private AiAnalysisRequest.NoteType toNoteType(MidiType type) {
        return switch (type) {
            case NOTE_ON -> AiAnalysisRequest.NoteType.NOTE_ON;
            case NOTE_OFF -> AiAnalysisRequest.NoteType.NOTE_OFF;
        };
    }

    private void validateBarRange(
            int startBar,
            int endBar,
            int totalBars
    ) {
        if (startBar > endBar) {
            throw new GeneralException(AnalysisErrorStatus.INVALID_BAR_ORDER);
        }
        int barCount = endBar - startBar + 1;
        if (barCount > MAX_BAR_COUNT || endBar > totalBars) {
            throw new GeneralException(AnalysisErrorStatus.INVALID_BAR_RANGE);
        }
    }

}
