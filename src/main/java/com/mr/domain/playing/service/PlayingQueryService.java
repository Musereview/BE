package com.mr.domain.playing.service;

import com.mr.domain.analysis.service.AnalysisBarCalculator;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.playing.dto.res.AnalysisContextResponse;
import com.mr.domain.playing.dto.res.PlayingDetailResponse;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.exception.PlayingErrorStatus;
import com.mr.domain.playing.repository.PlayingRepository;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.file.s3.enums.S3FileType;
import com.mr.global.file.s3.service.S3FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayingQueryService {

    private final PlayingRepository playingRepository;
    private final S3FileService s3FileService;
    private final AnalysisBarCalculator analysisBarCalculator;

    @Transactional(readOnly = true)
    public PlayingDetailResponse getPlayingDetail(Long userId, Long playingId) {
        validatePlayingId(playingId);

        Playing playing = playingRepository.findByIdWithBackingTrack(playingId)
                .orElseThrow(() -> new GeneralException(PlayingErrorStatus.PLAYING_NOT_FOUND));

        playing.validatePlayingOwner(userId);
        playing.validateCompleted();

        String recordingFileUrl =
                s3FileService.createPresignedDownload(
                        userId,
                        S3FileType.RECORDING,
                        playing.getRecordingObjectKey()
                );

        return PlayingDetailResponse.from(playing, recordingFileUrl);
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

        String recordingFileUrl =
                s3FileService.createPresignedDownload(
                        userId,
                        S3FileType.RECORDING,
                        playing.getRecordingObjectKey()
                );

        BackingTrack backingTrack = playing.getBackingTrack();

        String backingTrackAudioFileUrl = null;

        if (backingTrack.getAudioObjectKey() != null
                && !backingTrack.getAudioObjectKey().isBlank()) {

            backingTrackAudioFileUrl =
                    s3FileService.createPresignedDownload(
                            backingTrack.getUser().getUserId(),
                            S3FileType.BACKING_TRACK,
                            backingTrack.getAudioObjectKey()
                    );
        }

        int totalBars = analysisBarCalculator.calculate(playing).totalBars();
        return AnalysisContextResponse.from(playing, totalBars, recordingFileUrl, backingTrackAudioFileUrl);
    }

    private void validatePlayingId(Long playingId) {
        if (playingId == null || playingId < 1) {
            throw new GeneralException(PlayingErrorStatus.INVALID_PLAYING_ID);
        }
    }
}
