package com.mr.domain.backingTrack.controller;

import com.mr.domain.backingTrack.dto.req.BackingTrackSaveRequestDTO;
import com.mr.domain.backingTrack.dto.res.BackingTrackSaveResponseDTO;
import com.mr.domain.backingTrack.service.BackingTrackService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.principal.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/backing-tracks")
public class BackingTrackController {

    private final BackingTrackService backingTrackService;

    @PostMapping
    public ApiResponse<BackingTrackSaveResponseDTO.SaveResultDTO> createBackingTrack(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody BackingTrackSaveRequestDTO.SaveDTO request
    ) {
        BackingTrackSaveResponseDTO.SaveResultDTO result =
                backingTrackService.createBackingTrack(userDetails.getUserId(), request);

        return ApiResponse.onSuccess(result);
    }

    @PatchMapping("/{backingTrackId}")
    public ApiResponse<BackingTrackSaveResponseDTO.SaveResultDTO> updateBackingTrack(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long backingTrackId,
            @Valid @RequestBody BackingTrackSaveRequestDTO.SaveDTO request
    ) {
        Long userId = userDetails.getUserId();
        BackingTrackSaveResponseDTO.SaveResultDTO result =
                backingTrackService.updateBackingTrack(userId, backingTrackId, request);
        return ApiResponse.onSuccess(result);
    }
}
