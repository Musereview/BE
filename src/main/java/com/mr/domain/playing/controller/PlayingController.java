package com.mr.domain.playing.controller;

import com.mr.domain.playing.dto.req.MidiEventSaveRequest;
import com.mr.domain.playing.dto.req.PlayingStartRequest;
import com.mr.domain.playing.dto.req.RecordingUploadUrlRequest;
import com.mr.domain.playing.dto.res.MidiEventSaveResponse;
import com.mr.domain.playing.dto.res.AnalysisContextResponse;
import com.mr.domain.playing.dto.res.PlayingStartResponse;
import com.mr.domain.playing.dto.res.RecordingUploadUrlResponse;
import com.mr.domain.playing.service.PlayingService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.principal.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/playings")
@Tag(name = "연주", description = "연주 API")
public class PlayingController {

    private final PlayingService playingService;

    @Operation(
            summary = "연주 세션 시작 API",
            description = "백킹트랙을 기반으로 새로운 연주 세션을 생성하고 연주를 시작합니다."
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
            summary = "연주 녹음 파일 업로드 URL 발급 API",
            description = "연주 녹음 파일을 S3에 직접 업로드하기 위한 Presigned URL을 발급합니다."
    )
    @PostMapping("/{playingId}/recording-upload-url")
    public ApiResponse<RecordingUploadUrlResponse> createRecordingUploadUrl(
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(description = "연주 ID", example = "128")
            @PathVariable Long playingId,
            @Valid @RequestBody RecordingUploadUrlRequest request
    ) {
        Long userId = userDetails.getUserId();

        RecordingUploadUrlResponse response =
                playingService.createRecordingUploadUrl(userId, playingId, request);
        return ApiResponse.onSuccess(response);
    }

    @Operation(
            summary = "MIDI 이벤트 저장 및 연주 완료 API",
            description = "연주 중 수집한 MIDI 이벤트와 녹음 파일 정보를 저장하고 연주를 완료 처리합니다."
    )
    @PostMapping("/{playingId}/midi-events")
    public ApiResponse<MidiEventSaveResponse> saveMidiEvents(
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(description = "연주 ID", example = "128")
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
            summary = "분석 마디 선택 정보 조회 API",
            description = "완료된 본인 연주의 분석 마디 선택 정보를 조회합니다."
    )
    @GetMapping("/{playingId}/analysis-context")
    public ApiResponse<AnalysisContextResponse> getAnalysisContext(
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(description = "연주 ID", example = "128")
            @PathVariable Long playingId
    ) {
        AnalysisContextResponse response = playingService.getAnalysisContext(
                userDetails.getUserId(),
                playingId
        );

        return ApiResponse.onSuccess(response);
    }
}
