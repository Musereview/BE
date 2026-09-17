package com.mr.domain.playing.service;

import com.mr.domain.playing.dto.req.RecordingUploadUrlRequest;
import com.mr.domain.playing.dto.res.RecordingUploadUrlResponse;
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
public class PlayingFileService {

    private final PlayingRepository playingRepository;
    private final S3FileService s3FileService;

    @Transactional(readOnly = true)
    public RecordingUploadUrlResponse createRecordingUploadUrl(
            Long userId, Long playingId, RecordingUploadUrlRequest request
    ) {
        validatePlayingId(playingId);

        Playing playing = playingRepository.findByIdAndDeletedAtIsNull(playingId)
                .orElseThrow(() -> new GeneralException(PlayingErrorStatus.PLAYING_NOT_FOUND));

        playing.validatePlayingOwner(userId);
        playing.validateInProgress();

        return RecordingUploadUrlResponse.from(s3FileService.createPresignedUpload(
                userId, S3FileType.RECORDING, request.toCommand())
        );
    }

    private void validatePlayingId(Long playingId) {
        if (playingId == null || playingId < 1) {
            throw new GeneralException(PlayingErrorStatus.INVALID_PLAYING_ID);
        }
    }
}
