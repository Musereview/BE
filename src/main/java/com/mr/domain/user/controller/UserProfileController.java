package com.mr.domain.user.controller;

import com.mr.domain.user.dto.UserProfileRequestDTO;
import com.mr.domain.user.dto.UserProfileResponseDTO;
import com.mr.domain.user.service.UserProfileService;
import com.mr.global.apipayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    public ApiResponse<UserProfileResponseDTO.ProfileResponse> getMyProfile() {
        return ApiResponse.onSuccess(userProfileService.getMyProfile());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserProfileResponseDTO.OnboardingResponse> registerProfile(
            @RequestBody UserProfileRequestDTO.OnboardingRequest request
    ) {
        return ApiResponse.onSuccess(userProfileService.registerProfile(request));
    }

    @PatchMapping
    public ApiResponse<UserProfileResponseDTO.UpdateResponse> updateProfile(
            @RequestBody UserProfileRequestDTO.UpdateRequest request
    ) {
        return ApiResponse.onSuccess(userProfileService.updateProfile(request));
    }
}
