package com.mr.domain.analysis.service;

import com.mr.domain.analysis.exception.AnalysisErrorStatus;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.playing.entity.Playing;
import com.mr.global.apipayload.exception.GeneralException;
import org.springframework.stereotype.Component;

@Component
public class AnalysisBarCalculator {

    private static final double MILLIS_PER_MINUTE = 60_000D;

    public BarMetrics calculate(Playing playing) {
        BackingTrack track = playing.getBackingTrack();
        if (track == null
                || playing.getBpm() == null
                || playing.getBpm() <= 0
                || track.getPlaytimeSec() == null
                || track.getPlaytimeSec() <= 0) {
            throw new GeneralException(AnalysisErrorStatus.ANALYSIS_INVALID_REQUEST);
        }

        int[] timeSignature = parseTimeSignature(track.getTimeSignature());
        double barDurationMs = MILLIS_PER_MINUTE / playing.getBpm()
                * timeSignature[0] * 4D / timeSignature[1];
        int totalBars = (int) Math.ceil(
                track.getPlaytimeSec() * 1_000D / barDurationMs
        );

        return new BarMetrics(timeSignature, barDurationMs, totalBars);
    }

    private int[] parseTimeSignature(String value) {
        try {
            if (value == null) {
                throw new NumberFormatException();
            }
            String[] parts = value.split("/", -1);
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

    public record BarMetrics(
            int[] timeSignature,
            double barDurationMs,
            int totalBars
    ) {
    }
}
