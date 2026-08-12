package com.mr.domain.backingtrack.controller;

import com.mr.domain.backingtrack.dto.req.BackingTrackListRequestDTO;
import com.mr.domain.backingtrack.dto.req.BackingTrackSaveRequestDTO;
import com.mr.domain.backingtrack.dto.req.BackingTrackUploadUrlRequest;
import com.mr.domain.backingtrack.dto.req.PlayCountIncreaseRequestDTO;
import com.mr.domain.backingtrack.dto.res.BackingTrackCreateResponseDTO;
import com.mr.domain.backingtrack.dto.res.BackingTrackDetailResponseDTO;
import com.mr.domain.backingtrack.dto.res.BackingTrackListResponseDTO;
import com.mr.domain.backingtrack.dto.res.BackingTrackRecommendedResponseDTO;
import com.mr.domain.backingtrack.dto.res.BackingTrackUpdateResponseDTO;
import com.mr.domain.backingtrack.dto.res.BackingTrackUploadUrlResponse;
import com.mr.domain.backingtrack.dto.res.PlayCountIncreaseResponseDTO;
import com.mr.domain.backingtrack.service.BackingTrackService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.principal.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/backing-tracks")
@Tag(name = "백킹트랙", description = "백킹트랙 API")
public class BackingTrackController {

    private final BackingTrackService backingTrackService;

    @Operation(
            summary = "백킹트랙 생성 API",
            description = "트랙명, 장르, Key, 조성, BPM, 트랙 유형, 공개 범위, 난이도 등의 정보를 입력받아 저장"
    )
    @PostMapping
    public ApiResponse<BackingTrackCreateResponseDTO.CreateResultDTO> createBackingTrack(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody BackingTrackSaveRequestDTO.CreateDTO request
    ) {
        BackingTrackCreateResponseDTO.CreateResultDTO result =
                backingTrackService.createBackingTrack(userDetails.getUserId(), request);

        return ApiResponse.onSuccess(result);
    }

    @Operation(
            summary = "백킹트랙 오디오 파일 업로드 URL 발급 API",
            description = """
                백킹트랙 생성 시 첨부할 오디오 파일을 S3에 직접 업로드하기 위한
                Presigned PUT URL을 발급합니다.

                파일 업로드 후 반환된 objectKey를
                백킹트랙 생성 API의 audioObjectKey 필드로 전달합니다.
                """
    )
    @PostMapping("/audio-upload-url")
    public ApiResponse<BackingTrackUploadUrlResponse> createAudioUploadUrl(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody BackingTrackUploadUrlRequest request
    ) {
        return ApiResponse.onSuccess(
                backingTrackService.createAudioUploadUrl(
                        userDetails.getUserId(),
                        request
                )
        );
    }

    @Operation(
            summary = "백킹트랙 수정 API",
            description = "트랙의 생성자가 본인일 때, 트랙의 정보를 수정"
    )
    @PutMapping("/{backingTrackId}")
    public ApiResponse<BackingTrackUpdateResponseDTO.UpdateResultDTO> updateBackingTrack(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable @Min(value = 1, message = "BACKING_TRACK_400_23") Long backingTrackId,
            @Valid @RequestBody BackingTrackSaveRequestDTO.UpdateDTO request
    ) {
        Long userId = userDetails.getUserId();
        BackingTrackUpdateResponseDTO.UpdateResultDTO result =
                backingTrackService.updateBackingTrack(userId, backingTrackId, request);
        return ApiResponse.onSuccess(result);
    }

    @Operation(
            summary = "백킹트랙 재생 수 증가 API",
            description = "백킹트랙 기반 연주 후 AI 분석 응답 생성이 완료된 경우, 해당 백킹트랙의 재생 수를 1 증가"
    )
    @PatchMapping("/{backingTrackId}/play-count")
    public ApiResponse<PlayCountIncreaseResponseDTO.IncreaseResponseDTO> increasePlayCount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable @Min(value = 1, message = "BACKING_TRACK_400_23") Long backingTrackId,
            @RequestBody @Valid PlayCountIncreaseRequestDTO.IncreaseRequestDTO request
    ) {
        return ApiResponse.onSuccess(
                backingTrackService.increasePlayCount(backingTrackId, request, userDetails.getUserId())
        );
    }

    @Operation(
            summary = "백킹트랙 목록 조회 API",
            description = "사용자가 연주 가능한 백킹트랙 목록을 카드뷰 형태로 조회하는 API\n" +
                    "공개 범위와 사용자를 고려하여 필터 후 커서 기반 페이징"
    )
    @GetMapping
    public ApiResponse<BackingTrackListResponseDTO.ListResponseDTO> getBackingTracks(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute BackingTrackListRequestDTO.ListRequestDTO request
    ) {
        return ApiResponse.onSuccess(
                backingTrackService.getBackingTracks(request, userDetails.getUserId())
        );
    }

    @Operation(
            summary = "백킹트랙 상세 조회 API",
            description = "사용자가 선택한 백킹트랙의 상세 정보를 조회"
    )
    @GetMapping("/{backingTrackId}")
    public ApiResponse<BackingTrackDetailResponseDTO.DetailResponseDTO> getBackingTrackDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable @Min(value = 1, message = "BACKING_TRACK_400_23") Long backingTrackId
    ) {
        return ApiResponse.onSuccess(
                backingTrackService.getBackingTrackDetail(backingTrackId, userDetails.getUserId())
        );
    }

    @Operation(
            summary = "추천 백킹트랙 조회 API",
            description = "최근 1주일 기준으로 AI 분석이 완료(COMPLETED)된 백킹트랙 중 playCount가 높은 TOP3 백킹트랙을 조회\n" +
                    "playCount 내림차순으로 정렬"
    )
    @GetMapping("/recommended")
    public ApiResponse<BackingTrackRecommendedResponseDTO.RecommendedResponseDTO> getRecommendedTracks() {
        return ApiResponse.onSuccess(
                backingTrackService.getRecommendedTracks()
        );
    }
}
