package com.mr.domain.playing.controller;

import com.mr.domain.playing.dto.req.MidiEventSaveRequest;
import com.mr.domain.playing.dto.res.MidiEventSaveResponse;
import com.mr.domain.playing.service.PlayingService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.principal.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
}
