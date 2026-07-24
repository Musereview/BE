package com.mr.domain.user.controller;

import com.mr.domain.user.dto.UserProfileResponseDTO;
import com.mr.domain.user.service.UserProfileService;
import com.mr.global.apipayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
