package com.mr.domain.playing.controller;

import com.mr.domain.playing.dto.req.MidiEventSaveRequest;
import com.mr.domain.playing.dto.req.PlayingStartRequest;
import com.mr.domain.playing.dto.res.MidiEventSaveResponse;
import com.mr.domain.playing.dto.res.PlayingDeleteResponse;
import com.mr.domain.playing.dto.res.PlayingDetailResponse;
import com.mr.domain.playing.dto.res.PlayingStartResponse;
import com.mr.domain.playing.service.PlayingService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.file.s3.dto.req.RecordingPresignedUrlRequest;
import com.mr.global.file.s3.dto.res.RecordingPresignedUrlResponse;
import com.mr.global.security.principal.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
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

    @PostMapping("/{playingId}/recording-upload-url")
    public ApiResponse<RecordingPresignedUrlResponse> createRecordingUploadUrl(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long playingId,
            @Valid @RequestBody RecordingPresignedUrlRequest request
    ) {
        Long userId = userDetails.getUserId();

        RecordingPresignedUrlResponse response =
                playingService.createRecordingUploadUrl(userId, playingId, request);
        return ApiResponse.onSuccess(response);
    }

    @Operation(
            summary = "MIDI 이벤트 저장",
            description = "연주 세션에 대한 MIDI 이벤트를 저장하고 연주를 완료 상태로 변경합니다."
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
