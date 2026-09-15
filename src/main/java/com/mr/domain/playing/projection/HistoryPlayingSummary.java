package com.mr.domain.playing.projection;

import java.time.Instant;

public interface HistoryPlayingSummary {

    Long getPlayingId();
    Long getBackingTrackId();
    String getBackingTrackTitle();
    Integer getDurationSec();
    Instant getEndedAt();
}
