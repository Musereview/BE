package com.mr.domain.playing.service;

import com.mr.domain.playing.dto.req.MidiEventSaveRequest;
import com.mr.domain.playing.dto.res.MidiEventSaveResponse;
import com.mr.domain.playing.entity.MidiEventData;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import com.mr.domain.playing.exception.MidiEventErrorStatus;
import com.mr.domain.playing.exception.PlayingErrorStatus;
import com.mr.domain.playing.repository.PlayingRepository;
import com.mr.global.apipayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayingService {

    private static final long MIDI_SAVE_REQUEST_INTERVAL_MINUTES = 1L;
    private final PlayingRepository playingRepository;

    @Transactional
    public MidiEventSaveResponse saveMidiEvents(
            Long userId, Long playingId, MidiEventSaveRequest request
    ) {
        validatePlayingId(playingId);

        Playing playing = playingRepository.findByIdAndDeletedAtIsNull(playingId)
                .orElseThrow(() -> new GeneralException(PlayingErrorStatus.PLAYING_NOT_FOUND));

        playing.validatePlayingOwner(userId);
        validateMidiSaveRequestInterval(userId);

        List<MidiEventData> midiEvents = request.events()
                .stream()
                .map(event -> MidiEventData.of(
                        event.sequence(),
                        event.type(),
                        event.pitch(),
                        event.velocity(),
                        event.timestampMs()
                )).toList();

        playing.completeWithMidiData(midiEvents);

        return MidiEventSaveResponse.of(
                playing.getId(),
                playing.getMidiData().size()
        );
    }

    private void validatePlayingId(Long playingId) {
        if (playingId == null || playingId < 1 ) {
            throw new GeneralException(MidiEventErrorStatus.INVALID_PLAYING_ID);
        }
    }

    private void validateMidiSaveRequestInterval(Long userId) {
        LocalDateTime oneMinuteAgo =
                LocalDateTime.now()
                        .minusMinutes(
                                MIDI_SAVE_REQUEST_INTERVAL_MINUTES
                        );

        boolean recentlyCompleted =
                playingRepository
                        .existsByUser_UserIdAndStatusAndEndedAtAfterAndDeletedAtIsNull(
                                userId,
                                PlayingStatus.COMPLETED,
                                oneMinuteAgo
                        );

        if (recentlyCompleted) {
            throw new GeneralException(
                    MidiEventErrorStatus
                            .MIDI_SAVE_REQUEST_TOO_FREQUENT
            );
        }
    }
}
