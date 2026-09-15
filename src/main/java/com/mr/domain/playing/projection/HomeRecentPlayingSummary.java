package com.mr.domain.playing.projection;

import java.time.Instant;

public interface HomeRecentPlayingSummary {

    Long getPlayingId();
    String getBackingTrackTitle();
    String getBackingTrackGenre();
    String getBackingTrackKeySignature();
    Integer getBpm();
    Instant getEndedAt();
    Integer getDurationSec();
}
