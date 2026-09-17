package com.mr.domain.playing.service;

import com.mr.domain.playing.dto.req.RecordingUploadUrlRequest;
import com.mr.domain.playing.dto.res.RecordingUploadUrlResponse;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.exception.PlayingErrorStatus;
import com.mr.domain.playing.repository.PlayingRepository;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.file.s3.dto.FileUploadCommand;
import com.mr.global.file.s3.dto.PresignedUrlUpload;
import com.mr.global.file.s3.enums.S3FileType;
import com.mr.global.file.s3.service.S3FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayingFileServiceTest {

    private static final String RECORDING_OBJECT_KEY =
            "recordings/1/2026-08-02/150000_a1b2c3.mp3";

    @Mock
    private PlayingRepository playingRepository;

    @Mock
    private S3FileService s3FileService;

    @Mock
    private Playing playing;

    @InjectMocks
    private PlayingFileService playingFileService;

    private Long userId;
    private Long playingId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        playingId = 1L;
    }

    @Nested
    @DisplayName("녹음 파일 업로드 URL 발급")
    class CreateRecordingUploadUrl {

        @Test
        @DisplayName("진행 중인 본인의 연주이면 녹음 파일 업로드 URL을 발급한다")
        void createRecordingUploadUrl_success() {
            // given
            RecordingUploadUrlRequest request =
                    new RecordingUploadUrlRequest(
                            "recording.mp3",
                            "audio/mpeg",
                            1_024L
                    );

            FileUploadCommand command =
                    request.toCommand();

            PresignedUrlUpload presignedUpload =
                    new PresignedUrlUpload(
                            RECORDING_OBJECT_KEY,
                            "https://example.com/presigned-upload-url",
                            Instant.now().plusSeconds(600),
                            Map.of("Content-Type", "audio/mpeg")
                    );

            RecordingUploadUrlResponse expectedResponse =
                    RecordingUploadUrlResponse.from(
                            presignedUpload
                    );

            when(playingRepository.findByIdAndDeletedAtIsNull(playingId))
                    .thenReturn(Optional.of(playing));

            when(s3FileService.createPresignedUpload(
                    userId,
                    S3FileType.RECORDING,
                    command
            )).thenReturn(presignedUpload);

            // when
            RecordingUploadUrlResponse response =
                    playingFileService.createRecordingUploadUrl(
                            userId,
                            playingId,
                            request
                    );

            // then
            assertThat(response)
                    .isEqualTo(expectedResponse);

            verify(playingRepository)
                    .findByIdAndDeletedAtIsNull(playingId);

            verify(playing)
                    .validatePlayingOwner(userId);

            verify(playing)
                    .validateInProgress();

            verify(s3FileService)
                    .createPresignedUpload(userId, S3FileType.RECORDING, command);
        }

        @Test
        @DisplayName("진행 중이 아닌 연주에는 녹음 파일 업로드 URL을 발급하지 않는다")
        void createRecordingUploadUrl_notInProgress() {
            // given
            RecordingUploadUrlRequest request =
                    new RecordingUploadUrlRequest(
                            "recording.mp3",
                            "audio/mpeg",
                            1_024L
                    );

            when(playingRepository.findByIdAndDeletedAtIsNull(playingId))
                    .thenReturn(Optional.of(playing));

            doThrow(
                    new GeneralException(
                            PlayingErrorStatus.INVALID_PLAYING_STATUS
                    )
            )
                    .when(playing)
                    .validateInProgress();

            // when & then
            assertThatThrownBy(() ->
                    playingFileService.createRecordingUploadUrl(
                            userId,
                            playingId,
                            request

                    )
            )
                    .isInstanceOf(GeneralException.class)
                    .satisfies(exception -> {
                        GeneralException generalException =
                                (GeneralException) exception;

                        assertThat(generalException.getCode())
                                .isEqualTo(
                                        PlayingErrorStatus.INVALID_PLAYING_STATUS
                                );
                    });

            verify(playingRepository)
                    .findByIdAndDeletedAtIsNull(playingId);

            verify(playing)
                    .validatePlayingOwner(userId);

            verify(playing)
                    .validateInProgress();

            verify(s3FileService, never())
                    .createPresignedUpload(
                            anyLong(),
                            any(S3FileType.class),
                            any(FileUploadCommand.class)
                    );
        }
    }
}
