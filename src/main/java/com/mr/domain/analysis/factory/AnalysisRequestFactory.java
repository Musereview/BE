package com.mr.domain.analysis.factory;

import com.mr.domain.analysis.exception.AnalysisErrorStatus;
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
import org.springframework.stereotype.Component;

@Component
public class AnalysisRequestFactory {

    private static final int MAX_BAR_COUNT = 32;
    private static final double MILLIS_PER_MINUTE = 60_000D;

    public AiAnalysisRequest create(Playing playing, int startBar, int endBar) {
        BackingTrack track = playing.getBackingTrack();
        int[] timeSignature = parseTimeSignature(track.getTimeSignature());
        double barDurationMs = MILLIS_PER_MINUTE / playing.getBpm()
                * timeSignature[0] * 4D / timeSignature[1];

        validateBarRange(track, startBar, endBar, barDurationMs);

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
            BackingTrack track,
            int startBar,
            int endBar,
            double barDurationMs
    ) {
        if (startBar > endBar) {
            throw new GeneralException(AnalysisErrorStatus.INVALID_BAR_ORDER);
        }
        int barCount = endBar - startBar + 1;
        int totalBars = (int) Math.ceil(track.getPlaytimeSec() * 1_000D / barDurationMs);
        if (barCount > MAX_BAR_COUNT || endBar > totalBars) {
            throw new GeneralException(AnalysisErrorStatus.INVALID_BAR_RANGE);
        }
    }

    private int[] parseTimeSignature(String value) {
        try {
            String[] parts = value.split("/");
            if (parts.length != 2) {
                throw new NumberFormatException();
            }
            int numerator = Integer.parseInt(parts[0]);
            int denominator = Integer.parseInt(parts[1]);
            if (numerator <= 0 || denominator <= 0) {
                throw new NumberFormatException();
            }
            return new int[]{numerator, denominator};
        } catch (NumberFormatException exception) {
            throw new GeneralException(AnalysisErrorStatus.ANALYSIS_INVALID_REQUEST);
        }
    }
}
