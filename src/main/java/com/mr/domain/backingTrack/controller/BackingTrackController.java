package com.mr.domain.backingTrack.controller;

import com.mr.domain.backingTrack.dto.req.BackingTrackCreateRequestDTO;
import com.mr.domain.backingTrack.dto.res.BackingTrackCreateResponseDTO;
import com.mr.domain.backingTrack.service.BackingTrackService;
import com.mr.global.apipayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
            // 실제 인증 적용 시 @AuthUser / @AuthenticationPrincipal 등으로 유저 ID 주입
            @Valid @RequestBody BackingTrackCreateRequestDTO.CreateDTO request
    ) {
        Long userId = 1L; // 테스트용 임시 유저 ID

        BackingTrackCreateResponseDTO.CreateResultDTO result =
                backingTrackService.createBackingTrack(userId, request);

        return ApiResponse.onSuccess(result);
    }
}
