package com.mr.domain.auth.controller;

import com.mr.domain.auth.dto.AuthRequestDTO;
import com.mr.domain.auth.dto.AuthResponseDTO;
import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.domain.auth.service.AuthService;
import com.mr.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth API", description = "인증 및 소셜 로그인 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Profile({"local", "dev"})
public class AuthController {

    private final AuthService authService;

    @SecurityRequirements
    @Operation(
            summary = "소셜 로그인 / 회원가입 API (Authorization Code 또는 AccessToken)",
            description = "카카오 및 구글 OAuth 인가 코드(code) 또는 Access Token을 전달받아 서비스 전용 JWT 토큰을 발급합니다."
    )
    @PostMapping("/login/{socialType}")
    public ApiResponse<AuthResponseDTO.LoginResponse> socialLogin(
            @Parameter(description = "소셜 로그인 제공자 (KAKAO, GOOGLE)", example = "KAKAO")
            @PathVariable(name = "socialType") SocialType socialType,
            @RequestBody @Valid AuthRequestDTO.SocialLoginRequest request
    ) {
        AuthResponseDTO.LoginResponse response = authService.socialLogin(
                socialType,
                request.getEffectiveCodeOrToken(),
                request.redirectUri()
        );
        return ApiResponse.onSuccess(response);
    }
}