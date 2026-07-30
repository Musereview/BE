package com.mr.domain.auth.controller;

import com.mr.domain.auth.dto.OAuthCredential;
import com.mr.domain.auth.dto.req.AuthRequestDTO;
import com.mr.domain.auth.dto.res.AuthResponseDTO;
import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.domain.auth.exception.AuthErrorStatus;
import com.mr.domain.auth.service.AuthService;
import com.mr.domain.auth.service.OAuthClientService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth API", description = "인증 및 소셜 로그인 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OAuthClientService oAuthClientService;

    @SecurityRequirements
    @Operation(
            summary = "소셜 로그인 시작 API (Provider 로그인 이동)",
            description = "소셜 로그인 제공자(카카오/구글)의 OAuth 인가 페이지로 HTTP 302 리다이렉트합니다."
    )
    @GetMapping("/login/{socialType}")
    public void startOAuthLogin(
            @Parameter(description = "소셜 로그인 제공자 (KAKAO, GOOGLE)", example = "KAKAO")
            @PathVariable(name = "socialType") SocialType socialType,
            @Parameter(description = "커스텀 리다이렉트 URI (선택)", example = "http://localhost:8080/api/auth/kakao/callback")
            @RequestParam(name = "redirectUri", required = false) String customRedirectUri,
            HttpServletResponse response
    ) throws IOException {
        String authUrl = oAuthClientService.getAuthorizationUrl(socialType, customRedirectUri);
        response.sendRedirect(authUrl);
    }

    @SecurityRequirements
    @Operation(
            summary = "소셜 로그인 백엔드 콜백 API (OAuth Server ➔ Backend)",
            description = "OAuth 제공자로부터 인가 코드(code)를 받아 토큰 교환 및 소셜 로그인을 수행한 후 프론트엔드 페이지로 리다이렉트합니다."
    )
    @GetMapping("/{socialType}/callback")
    public void oAuthCallback(
            @Parameter(description = "소셜 로그인 제공자 (KAKAO, GOOGLE)", example = "KAKAO")
            @PathVariable(name = "socialType") SocialType socialType,
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "redirectUri", required = false) String customRedirectUri,
            @RequestHeader(value = HttpHeaders.USER_AGENT, defaultValue = "Unknown Device") String deviceInfo,
            HttpServletResponse response
    ) throws IOException {
        if (error != null || code == null || code.isBlank()) {
            throw new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST);
        }

        OAuthCredential credential = new OAuthCredential(OAuthCredential.CredentialType.AUTHORIZATION_CODE, code);
        AuthResponseDTO.LoginResponse loginResponse = authService.socialLogin(
                socialType,
                credential,
                customRedirectUri,
                deviceInfo
        );

        String targetFrontendUrl = oAuthClientService.buildFrontendRedirectUrl(loginResponse);
        response.sendRedirect(targetFrontendUrl);
    }

    @SecurityRequirements
    @Operation(
            summary = "소셜 로그인 / 회원가입 API (Authorization Code 또는 AccessToken)",
            description = "카카오 및 구글 OAuth 인가 코드(code) 또는 Access Token을 전달받아 서비스 전용 JWT 토큰을 발급합니다."
    )
    @PostMapping("/login/{socialType}")
    public ApiResponse<AuthResponseDTO.LoginResponse> socialLogin(
            @Parameter(description = "소셜 로그인 제공자 (KAKAO, GOOGLE)", example = "KAKAO")
            @PathVariable(name = "socialType") SocialType socialType,
            @RequestBody @Valid AuthRequestDTO.SocialLoginRequest request,
            @RequestHeader(value = HttpHeaders.USER_AGENT, defaultValue = "Unknown Device") String deviceInfo
    ) {
        AuthResponseDTO.LoginResponse response = authService.socialLogin(
                socialType,
                request.getCredential(),
                request.redirectUri(),
                deviceInfo
        );
        return ApiResponse.onSuccess(response);
    }

    @SecurityRequirements
    @Operation(summary = "토큰 재발급 API", description = "만료된 Access Token을 Refresh Token을 이용해 재발급합니다.")
    @PostMapping("/reissue")
    public ApiResponse<AuthResponseDTO.TokenInfo> reissue(
            @RequestBody @Valid AuthRequestDTO.TokenRefreshRequest request
    ) {
        AuthResponseDTO.TokenInfo tokenInfo = authService.reissueToken(request.refreshToken());
        return ApiResponse.onSuccess(tokenInfo);
    }

    @Operation(
            summary = "소셜 계정 추가 연동 API",
            description = "현재 로그인된 사용자의 계정에 새로운 소셜 계정(카카오/구글)을 추가로 연동합니다."
    )
    @PostMapping("/link/{socialType}")
    public ApiResponse<AuthResponseDTO.TokenInfo> linkSocialAccount(
            @Parameter(description = "연동할 소셜 제공자 (KAKAO, GOOGLE)", example = "GOOGLE")
            @PathVariable(name = "socialType") SocialType socialType,
            @RequestBody @Valid AuthRequestDTO.SocialLoginRequest request,
            @RequestHeader(value = HttpHeaders.USER_AGENT, defaultValue = "Unknown Device") String deviceInfo
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        AuthResponseDTO.TokenInfo tokenInfo = authService.linkSocialAccount(
                userId,
                socialType,
                request.getCredential(),
                request.redirectUri(),
                deviceInfo
        );
        return ApiResponse.onSuccess(tokenInfo);
    }

    @Operation(
            summary = "로그아웃 API",
            description = "현재 요청 기기의 Refresh Token 세션을 선택적으로 만료 처리합니다."
    )
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestBody @Valid AuthRequestDTO.LogoutRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        authService.logout(userId, request.refreshToken());
        return ApiResponse.onSuccess(null);
    }

    @Operation(
            summary = "회원 탈퇴 API",
            description = "사용자 계정을 탈퇴 처리하고 저장된 소셜 인증 정보 및 Refresh Token 세션을 완전히 삭제합니다."
    )
    @PostMapping("/withdraw")
    public ApiResponse<Void> withdraw() {
        Long userId = SecurityUtil.getCurrentUserId();
        authService.withdraw(userId);
        return ApiResponse.onSuccess(null);
    }
}