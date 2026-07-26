package com.mr.domain.user.controller;

import com.mr.domain.user.dto.req.UserProfileRequestDTO;
import com.mr.domain.user.dto.res.UserProfileResponseDTO;
import com.mr.domain.user.service.UserProfileService;
import com.mr.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "사용자 프로필", description = "프로필 조회/최초 등록(온보딩)/수정 API")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Operation(
            summary = "프로필 조회",
            description = "현재 로그인한 사용자의 닉네임, 프로필 이미지, 대표 악기, 화성학 숙련도, 구독 등급, 누적 학습 통계를 조회합니다."
    )
    @GetMapping
    public ApiResponse<UserProfileResponseDTO.ProfileResponse> getMyProfile() {
        return ApiResponse.onSuccess(userProfileService.getMyProfile());
    }

    @Operation(
            summary = "프로필 최초 등록 (온보딩)",
            description = "닉네임, 화성학 숙련도를 입력받아 Student/StudentInstrument(대표 악기 PIANO)/Subscription을 하나의 트랜잭션으로 생성합니다. "
                    + "구독 등급은 MVP 기간 동안 결제 없이 항상 PRO로 서버에서 고정됩니다. "
                    + "이미 온보딩을 완료한 사용자가 다시 호출하면 409(USER_409_02)가 반환됩니다."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserProfileResponseDTO.OnboardingResponse> registerProfile(
            @RequestBody UserProfileRequestDTO.OnboardingRequest request
    ) {
        return ApiResponse.onSuccess(userProfileService.registerProfile(request));
    }

    @Operation(
            summary = "프로필 수정",
            description = "닉네임과 화성학 숙련도만 수정합니다. 대표 악기, 프로필 이미지, 구독 등급은 이 API에서 변경되지 않습니다."
    )
    @PatchMapping
    public ApiResponse<UserProfileResponseDTO.UpdateResponse> updateProfile(
            @RequestBody UserProfileRequestDTO.UpdateRequest request
    ) {
        return ApiResponse.onSuccess(userProfileService.updateProfile(request));
    }
}
