package com.mr.domain.backingTrack.controller;

import com.mr.domain.backingTrack.dto.req.BackingTrackCreateRequestDTO;
import com.mr.domain.backingTrack.dto.res.BackingTrackCreateResponseDTO;
import com.mr.domain.backingTrack.service.BackingTrackService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.principal.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ApiResponse<BackingTrackCreateResponseDTO.CreateResultDTO> createBackingTrack(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody BackingTrackCreateRequestDTO.CreateDTO request
    ) {
        BackingTrackCreateResponseDTO.CreateResultDTO result =
                backingTrackService.createBackingTrack(userDetails.getUserId(), request);

        return ApiResponse.onSuccess(result);
    }
}
