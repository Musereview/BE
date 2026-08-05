package com.mr.domain.playing.service;

import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.backingtrack.repository.BackingTrackRepository;
import com.mr.domain.analysis.service.AnalysisBarCalculator;
import com.mr.domain.playing.dto.req.MidiEventSaveRequest;
import com.mr.domain.playing.dto.req.PlayingStartRequest;
import com.mr.domain.playing.dto.res.MidiEventSaveResponse;
import com.mr.domain.playing.dto.res.AnalysisContextResponse;
import com.mr.domain.playing.dto.res.PlayingDeleteResponse;
import com.mr.domain.playing.dto.res.PlayingDetailResponse;
import com.mr.domain.playing.dto.res.PlayingStartResponse;
import com.mr.domain.playing.entity.MidiEventData;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.exception.PlayingErrorStatus;
import com.mr.domain.playing.repository.PlayingRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.event.PlayingCompletedEvent;
import com.mr.global.file.s3.dto.ValidatedS3Object;
import com.mr.global.file.s3.dto.req.RecordingPresignedUrlRequest;
import com.mr.global.file.s3.dto.res.RecordingPresignedUrlResponse;
import com.mr.global.file.s3.service.RecordingUploadService;
import com.mr.global.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.mr.domain.backingtrack.entity.enums.AccessLevel.PUBLIC;

@Service
@RequiredArgsConstructor
public class PlayingService {

    private final PlayingRepository playingRepository;
    private final AnalysisBarCalculator analysisBarCalculator;
    private final UserRepository userRepository;
    private final BackingTrackRepository backingTrackRepository;
    private final RecordingUploadService recordingUploadService;
    private final TransactionTemplate transactionTemplate;

    private final ApplicationEventPublisher eventPublisher;

    private final Clock clock;

    @Transactional
    public PlayingStartResponse startPlaying(
            Long userId, PlayingStartRequest request
    ) {
        validateUserId(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(PlayingErrorStatus.USER_NOT_FOUND));

        BackingTrack backingTrack = backingTrackRepository.findByIdWithChordProgressions(request.backingTrackId())
                .orElseThrow(() -> new GeneralException(PlayingErrorStatus.BACKING_TRACK_NOT_FOUND));

        validateBackingTrackAccessible(userId, backingTrack);

        Playing playing = Playing.createBackingTrack(
                user, backingTrack, backingTrack.getBpm()
        );

        playing.start();
        Playing savedPlaying = playingRepository.save(playing);

        return PlayingStartResponse.from(savedPlaying);

    }

    @Transactional(readOnly = true)
    public RecordingPresignedUrlResponse createRecordingUploadUrl(
            Long userId, Long playingId, RecordingPresignedUrlRequest request
    ) {
        validatePlayingId(playingId);

        Playing playing = playingRepository.findByIdAndDeletedAtIsNull(playingId)
                .orElseThrow(() -> new GeneralException(PlayingErrorStatus.PLAYING_NOT_FOUND));

        playing.validatePlayingOwner(userId);
        playing.validateInProgress();

        return recordingUploadService.createPresignedUrl(userId, request);
    }

    public MidiEventSaveResponse saveMidiEvents(
            Long userId, Long playingId, MidiEventSaveRequest request
    ) {
        validatePlayingId(playingId);

        // S3 네트워크 통신은 DB 트랜잭션 밖에서 수행
        ValidatedS3Object recording =
                recordingUploadService.validateObject(userId, request.recordingObjectKey());

        List<MidiEventData> midiEvents = request.events()
                .stream()
                .map(event -> MidiEventData.of(
                        event.sequence(),
                        event.type(),
                        event.pitch(),
                        event.velocity(),
                        event.timestampMs()
                )).toList();

        // DB 조회 및 변경 작업만 트랜잭션 안에서 수행
        return transactionTemplate.execute(status -> {
            Playing playing = playingRepository.findByIdAndDeletedAtIsNull(playingId)
                    .orElseThrow(() -> new GeneralException(PlayingErrorStatus.PLAYING_NOT_FOUND));

            playing.validatePlayingOwner(userId);
            playing.validateInProgress();

            playing.completeWithMidiData(
                    midiEvents,
                    recording.fileUrl()
            );

            publishPracticeMilestoneNotification(userId, playing);

            eventPublisher.publishEvent(
                    PlayingCompletedEvent.of(userId)
            );

            return MidiEventSaveResponse.of(
                    playing.getId(),
                    playing.getMidiData().size()
            );
        });
    }

    @Transactional(readOnly = true)
    public PlayingDetailResponse getPlayingDetail(Long userId, Long playingId) {
        validatePlayingId(playingId);

        Playing playing = playingRepository.findByIdWithBackingTrack(playingId)
                .orElseThrow(() -> new GeneralException(PlayingErrorStatus.PLAYING_NOT_FOUND));

        playing.validatePlayingOwner(userId);
        playing.validateCompleted();

        return PlayingDetailResponse.from(playing);
    }

    @Transactional(readOnly = true)
    public AnalysisContextResponse getAnalysisContext(Long userId, Long playingId) {
        validatePlayingId(playingId);

        Playing playing = playingRepository.findByIdWithBackingTrack(playingId)
                .orElseThrow(() -> new GeneralException(PlayingErrorStatus.PLAYING_NOT_FOUND));

        playing.validatePlayingOwner(userId);
        playing.validateCompleted();
        if (playing.getBackingTrack() == null) {
            throw new GeneralException(PlayingErrorStatus.BACKING_TRACK_NOT_FOUND);
        }

        int totalBars = analysisBarCalculator.calculate(playing).totalBars();
        return AnalysisContextResponse.from(playing, totalBars);
    }

    @Transactional
    public PlayingDeleteResponse deletePlaying(Long userId, Long playingId) {

        validatePlayingId(playingId);

        Playing playing = playingRepository.findByIdAndDeletedAtIsNull(playingId)
                .orElseThrow(() -> new GeneralException(PlayingErrorStatus.PLAYING_NOT_FOUND));

        playing.validatePlayingOwner(userId);
        playing.softDelete();

        return PlayingDeleteResponse.from(playing);
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId < 1) {
            throw new GeneralException(PlayingErrorStatus.MISSING_USER_ID);
        }
    }

    private void validatePlayingId(Long playingId) {
        if (playingId == null || playingId < 1 ) {
            throw new GeneralException(PlayingErrorStatus.INVALID_PLAYING_ID);
        }
    }

    private void validateBackingTrackAccessible(Long userId, BackingTrack backingTrack) {
        if (backingTrack.getAccessLevel() == PUBLIC){
            return;
        }

        // TODO:
        // ACADEMY 접근 정책은 User-학원 관계가 추가되면
        // 같은 학원 사용자에게 접근을 허용하도록 수정한다.
        // 현재는 PRIVATE와 동일하게 생성자만 접근 가능하도록 처리한다.

        if (backingTrack.getUser() == null || !backingTrack.getUser().getUserId().equals(userId)){
            throw new GeneralException(PlayingErrorStatus.BACKING_TRACK_ACCESS_FORBIDDEN);
        }
    }

    private void publishPracticeMilestoneNotification(
            Long userId, Playing playing
    ) {
        int intervalHours = 10; // 10시간 단위로 알림
        int intervalSeconds = intervalHours * 3600;

        LocalDateTime weekStart = LocalDate.now(clock).with(DayOfWeek.MONDAY).atStartOfDay();

        // 방금 끝낸 연주를 제외한 이전 누적 시간
        Long previousWeeklySeconds = playingRepository.sumDurationSecExcludeCurrent(
                userId, playing.getStatus(), weekStart, playing.getId()
        );

        Long currentDuration = playing.getDurationSec() != null ? playing.getDurationSec() : 0L;
        Long totalWeeklySeconds = previousWeeklySeconds + currentDuration;

        Long previousMilestones = previousWeeklySeconds / intervalSeconds;
        Long currentMilestones = totalWeeklySeconds / intervalSeconds;

        if (currentMilestones > previousMilestones) {
            int achievedHours = (int)(currentMilestones * intervalHours); // 달성한 시간: 10 시간 단위
            eventPublisher.publishEvent(
                    NotificationEvent.forPractice(userId, playing.getUser().getNickname(), achievedHours)
            );
        }
    }
}
