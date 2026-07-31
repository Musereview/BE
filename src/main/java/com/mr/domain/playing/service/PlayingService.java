package com.mr.domain.playing.service;

import com.mr.domain.backingTrack.entity.BackingTrack;
import com.mr.domain.backingTrack.repository.BackingTrackRepository;
import com.mr.domain.playing.dto.req.MidiEventSaveRequest;
import com.mr.domain.playing.dto.req.PlayingStartRequest;
import com.mr.domain.playing.dto.res.MidiEventSaveResponse;
import com.mr.domain.playing.dto.res.PlayingStartResponse;
import com.mr.domain.playing.entity.MidiEventData;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import com.mr.domain.playing.exception.MidiEventErrorStatus;
import com.mr.domain.playing.exception.PlayingErrorStatus;
import com.mr.domain.playing.repository.PlayingRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.mr.domain.backingTrack.entity.enums.AccessLevel.PUBLIC;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayingService {

    private static final long MIDI_SAVE_REQUEST_INTERVAL_MINUTES = 1L;
    private final PlayingRepository playingRepository;
    private final UserRepository userRepository;
    private final BackingTrackRepository backingTrackRepository;

    @Transactional
    public PlayingStartResponse startPlaying(
            Long userId, PlayingStartRequest request
    ) {
        validateUserId(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(PlayingErrorStatus.USER_NOT_FOUND));

        BackingTrack backingTrack = backingTrackRepository.findByIdAndDeletedAtIsNull(request.backingTrackId())
                .orElseThrow(() -> new GeneralException(PlayingErrorStatus.BACKING_TRACK_NOT_FOUND));

        validateBackingTrackAccessible(userId, backingTrack);

        Playing playing = Playing.createBackingTrack(
                user, backingTrack, backingTrack.getBpm()
        );

        playing.start();
        Playing savedPlaying = playingRepository.save(playing);

        return PlayingStartResponse.from(savedPlaying);


    }

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

    private void validateUserId(Long userId) {
        if (userId == null || userId < 1) {
            throw new GeneralException(PlayingErrorStatus.MISSING_USER_ID);
        }
    }

    private void validatePlayingId(Long playingId) {
        if (playingId == null || playingId < 1 ) {
            throw new GeneralException(MidiEventErrorStatus.INVALID_PLAYING_ID);
        }
    }

    private void validateBackingTrackAccessible(Long userId, BackingTrack backingTrack) {
        if (backingTrack.getAccessLevel() == PUBLIC){
            return;
        }

        if (backingTrack.getUser() == null || !backingTrack.getUser().getUserId().equals(userId)){
            throw new GeneralException(PlayingErrorStatus.BACKING_TRACK_ACCESS_FORBIDDEN);
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
