package com.mr.domain.playing.controller;

import com.mr.domain.playing.dto.req.MidiEventSaveRequest;
import com.mr.domain.playing.dto.req.PlayingStartRequest;
import com.mr.domain.playing.dto.req.RecordingUploadUrlRequest;
import com.mr.domain.playing.dto.res.MidiEventSaveResponse;
import com.mr.domain.playing.dto.res.PlayingDeleteResponse;
import com.mr.domain.playing.dto.res.PlayingDetailResponse;
import com.mr.domain.playing.dto.res.PlayingStartResponse;
import com.mr.domain.playing.dto.res.RecordingUploadUrlResponse;
import com.mr.domain.playing.service.PlayingService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.principal.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/playings")
@Tag(name = "연주", description = "연주 세션 생성, 파일 업로드, MIDI 저장, 연주 기록 조회 및 삭제 API")
public class PlayingController {

    private final PlayingService playingService;

    @Operation(
            summary = "연주 세션 시작",
            description = "백킹트랙 기반 연주 세션을 생성하고 연주 시작 상태로 변경합니다."
    )
    @PostMapping
    public ApiResponse<PlayingStartResponse> startPlaying(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PlayingStartRequest request
    ){
        Long userId = userDetails.getUserId();

        PlayingStartResponse response = playingService.startPlaying(userId, request);
        return ApiResponse.onSuccess(response);
    }

    @Operation(
            summary = "연주 녹음 파일 업로드 URL 발급",
            description = """
                    연주 녹음 파일을 S3에 직접 업로드하기 위한 Presigned URL을 발급합니다.
                    로그인한 사용자 본인의 IN_PROGRESS 상태 연주 세션에 대해서만 요청할 수 있습니다.
                    발급받은 URL을 사용하여 클라이언트가 녹음 파일을 S3에 직접 업로드합니다.
                    """
    )
    @PostMapping("/{playingId}/recording-upload-url")
    public ApiResponse<RecordingUploadUrlResponse> createRecordingUploadUrl(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long playingId,
            @Valid @RequestBody RecordingUploadUrlRequest request
    ) {
        Long userId = userDetails.getUserId();

        RecordingUploadUrlResponse response =
                playingService.createRecordingUploadUrl(userId, playingId, request);
        return ApiResponse.onSuccess(response);
    }

    @Operation(
            summary = "MIDI 이벤트 저장 및 연주 완료",
            description = """
                    연주 중 수집한 MIDI 이벤트와 업로드된 녹음 파일 정보를 저장합니다.
                    로그인한 사용자 본인의 IN_PROGRESS 상태 연주 세션에 대해서만 요청할 수 있습니다.
                    MIDI 이벤트와 녹음 파일 검증이 완료되면 연주 세션을 COMPLETED 상태로 변경합니다.
                    연주 완료 트랜잭션이 정상적으로 커밋된 이후 사용자 연습 통계가 갱신됩니다.
                    """
    )
    @PostMapping("/{playingId}/midi-events")
    public ApiResponse<MidiEventSaveResponse> saveMidiEvents(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long playingId,
            @Valid @RequestBody MidiEventSaveRequest request
    ){
        Long userId = userDetails.getUserId();

        MidiEventSaveResponse response = playingService.saveMidiEvents(
                userId,
                playingId,
                request
        );

        return ApiResponse.onSuccess(response);
    }

    @Operation(
            summary = "연주 세션 단건 조회",
            description = """
                           완료된 연주 세션의 상세 정보를 조회합니다.
                           로그인한 사용자 본인의 연주 세션만 조회할 수 있으며,
                           삭제되지 않은 COMPLETED 상태의 연주 세션만 조회할 수 있습니다.
                           """
    )
    @GetMapping("/{playingId}")
    public ApiResponse<PlayingDetailResponse> getPlayingDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long playingId
    ){
        Long userId = userDetails.getUserId();

        PlayingDetailResponse response = playingService.getPlayingDetail(userId, playingId);

        return ApiResponse.onSuccess(response);
    }

    @Operation(
            summary = "연주 기록 삭제",
            description = """
                            로그인한 사용자의 연주 기록을 삭제합니다.
                            본인의 삭제되지 않은 연주 기록만 삭제할 수 있으며,
                            실제 데이터는 제거하지 않고 삭제 일시를 기록하는 Soft Delete 방식으로 처리합니다.
                            """
    )
    @DeleteMapping("/{playingId}")
    public ApiResponse<PlayingDeleteResponse> deletePlaying(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long playingId
    ){

        Long userId = userDetails.getUserId();

        PlayingDeleteResponse response = playingService.deletePlaying(userId, playingId);

        return ApiResponse.onSuccess(response);
    }
}
