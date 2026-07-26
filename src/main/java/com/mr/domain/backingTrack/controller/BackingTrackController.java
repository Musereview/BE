package com.mr.domain.backingTrack.controller;

import com.mr.domain.backingTrack.dto.req.BackingTrackSaveRequestDTO;
import com.mr.domain.backingTrack.dto.req.PlayCountIncreaseRequestDTO;
import com.mr.domain.backingTrack.dto.res.BackingTrackCreateResponseDTO;
import com.mr.domain.backingTrack.dto.res.BackingTrackUpdateResponseDTO;
import com.mr.domain.backingTrack.dto.res.PlayCountIncreaseResponseDTO;
import com.mr.domain.backingTrack.service.BackingTrackService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.principal.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
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
public class BackingTrackController {

    private final BackingTrackService backingTrackService;

    @PostMapping
    public ApiResponse<BackingTrackCreateResponseDTO.CreateResultDTO> createBackingTrack(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody BackingTrackSaveRequestDTO.SaveDTO request
    ) {
        BackingTrackCreateResponseDTO.CreateResultDTO result =
                backingTrackService.createBackingTrack(userDetails.getUserId(), request);

        return ApiResponse.onSuccess(result);
    }

    @PutMapping("/{backingTrackId}")
    public ApiResponse<BackingTrackUpdateResponseDTO.UpdateResultDTO> updateBackingTrack(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long backingTrackId,
            @Valid @RequestBody BackingTrackSaveRequestDTO.SaveDTO request
    ) {
        Long userId = userDetails.getUserId();
        BackingTrackUpdateResponseDTO.UpdateResultDTO result =
                backingTrackService.updateBackingTrack(userId, backingTrackId, request);
        return ApiResponse.onSuccess(result);
    }

    @PatchMapping("/{backingTrackId}/play-count")
    public ApiResponse<PlayCountIncreaseResponseDTO.IncreaseResponseDTO> increasePlayCount(
            @PathVariable @Min(value = 1, message = "BACKING_TRACK_400_23") Long backingTrackId,
            @RequestBody @Valid PlayCountIncreaseRequestDTO.IncreaseRequestDTO request
    ) {
        return ApiResponse.onSuccess(
                backingTrackService.increasePlayCount(backingTrackId, request)
        );
    }
}
