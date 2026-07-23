package com.mr.domain.auth.controller;

import com.mr.domain.auth.dto.AuthRequestDTO;
import com.mr.domain.auth.dto.AuthResponseDTO;
import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.domain.auth.service.AuthService;
import com.mr.global.apipayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login/{socialType}")
    public ApiResponse<AuthResponseDTO.LoginResponse> socialLogin(
            @PathVariable(name = "socialType") SocialType socialType,
            @RequestBody @Valid AuthRequestDTO.SocialLoginRequest request
    ) {
        AuthResponseDTO.LoginResponse response = authService.socialLogin(socialType, request.accessToken());
        return ApiResponse.onSuccess(response);
    }
}