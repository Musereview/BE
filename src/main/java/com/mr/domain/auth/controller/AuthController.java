package com.mr.domain.auth.controller;

import com.mr.domain.auth.dto.req.AuthRequestDTO;
import com.mr.domain.auth.dto.res.AuthResponseDTO;
import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.domain.auth.service.AuthService;
import com.mr.domain.auth.service.OAuthClientService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Tag(name = "Auth API", description = "인증/인가 관련 API (소셜 로그인, 토큰 재발급, 로그아웃)")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OAuthClientService oAuthClientService;

    @SecurityRequirements
    @Operation(
            summary = "소셜 로그인 시작 (OAuth 인가 URL 반환)",
            description = "선택한 소셜 제공자(카카오/구글)의 로그인 페이지 URL로 302 리다이렉트합니다."
    )
    @GetMapping("/login/{socialType}")
    public void startOAuthLogin(
            @Parameter(description = "소셜 로그인 제공자 (KAKAO, GOOGLE)", example = "KAKAO")
            @PathVariable(name = "socialType") SocialType socialType,
            @Parameter(description = "커스텀 리다이렉트 URI (선택)", example = "http://localhost:8080/api/auth/kakao/callback")
            @RequestParam(name = "redirectUri", required = false) String customRedirectUri,
            HttpServletResponse response
    ) throws IOException {
        String state = UUID.randomUUID().toString();

        ResponseCookie stateCookie = ResponseCookie.from("oauth_state", state)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(300)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, stateCookie.toString());

        if (customRedirectUri != null && !customRedirectUri.isBlank()) {
            ResponseCookie redirectCookie = ResponseCookie.from("oauth_redirect_uri", customRedirectUri)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(300)
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, redirectCookie.toString());
        }

        String authUrl = oAuthClientService.getAuthorizationUrl(socialType, customRedirectUri, state);
        response.sendRedirect(authUrl);
    }

    @SecurityRequirements
    @Operation(
            summary = "OAuth 콜백 수신 엔드포인트",
            description = "소셜 로그인 완료 후 Provider가 인가 코드(code)와 state를 백엔드로 전달하는 콜백 API입니다."
    )
    @GetMapping("/{socialType}/callback")
    public void oAuthCallback(
            @Parameter(description = "소셜 로그인 제공자 (KAKAO, GOOGLE)", example = "KAKAO")
            @PathVariable(name = "socialType") SocialType socialType,
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "redirectUri", required = false) String customRedirectUri,
            @RequestHeader(value = HttpHeaders.USER_AGENT, defaultValue = "Unknown Device") String deviceInfo,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        String savedState = null;
        String savedCustomRedirectUri = null;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("oauth_state".equals(cookie.getName())) {
                    savedState = cookie.getValue();
                } else if ("oauth_redirect_uri".equals(cookie.getName())) {
                    savedCustomRedirectUri = cookie.getValue();
                }
            }
        }

        ResponseCookie clearStateCookie = ResponseCookie.from("oauth_state", "")
                .httpOnly(true).secure(false).path("/").maxAge(0).sameSite("Lax").build();
        ResponseCookie clearRedirectCookie = ResponseCookie.from("oauth_redirect_uri", "")
                .httpOnly(true).secure(false).path("/").maxAge(0).sameSite("Lax").build();
        response.addHeader(HttpHeaders.SET_COOKIE, clearStateCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearRedirectCookie.toString());

        String effectiveRedirectUri = (savedCustomRedirectUri != null && !savedCustomRedirectUri.isBlank())
                ? savedCustomRedirectUri
                : customRedirectUri;

        String backendOAuthRedirectUri = null;
        String frontendTargetRedirectUri = null;

        if (effectiveRedirectUri != null && !effectiveRedirectUri.isBlank()) {
            if (oAuthClientService.isBackendAllowedRedirectUri(socialType, effectiveRedirectUri)) {
                backendOAuthRedirectUri = effectiveRedirectUri;
            } else {
                frontendTargetRedirectUri = effectiveRedirectUri;
            }
        }

        if (error != null || code == null || code.isBlank()) {
            log.warn("{} OAuth login failed/cancelled by user: error={}", socialType, error);
            String errorCode = "access_denied".equalsIgnoreCase(error) ? "access_denied" : "oauth_failed";
            String errorRedirectUrl = oAuthClientService.buildFrontendErrorRedirectUrl(errorCode, frontendTargetRedirectUri);
            response.sendRedirect(errorRedirectUrl);
            return;
        }

        if (savedState != null && (state == null || !savedState.equals(state))) {
            log.warn("{} OAuth state mismatch: saved={}, received={}", socialType, savedState, state);
            String errorRedirectUrl = oAuthClientService.buildFrontendErrorRedirectUrl("invalid_state", frontendTargetRedirectUri);
            response.sendRedirect(errorRedirectUrl);
            return;
        }

        if (savedState == null) {
            log.warn("{} OAuth state cookie missing in callback request", socialType);
            String errorRedirectUrl = oAuthClientService.buildFrontendErrorRedirectUrl("invalid_auth_request", frontendTargetRedirectUri);
            response.sendRedirect(errorRedirectUrl);
            return;
        }

        try {
            AuthResponseDTO.LoginResponse loginResponse = authService.socialLoginByCode(
                    socialType,
                    code,
                    backendOAuthRedirectUri,
                    deviceInfo
            );

            String tempCode = authService.generateTempExchangeCode(loginResponse);
            String targetFrontendUrl = oAuthClientService.buildFrontendRedirectUrl(tempCode, frontendTargetRedirectUri);
            response.sendRedirect(targetFrontendUrl);
        } catch (Exception e) {
            String errorCode = (e instanceof GeneralException ge && ge.getCode() != null)
                    ? ge.getCode().getCode()
                    : "authentication_failed";
            String errorRedirectUrl = oAuthClientService.buildFrontendErrorRedirectUrl(errorCode, frontendTargetRedirectUri);
            response.sendRedirect(errorRedirectUrl);
        }
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @SecurityRequirements
    @Operation(
            summary = "임시 교환 코드로 JWT 토큰 발급 API",
            description = "소셜 로그인 리다이렉트 콜백에서 전달받은 1회성 임시 코드(code)를 사용하여 서비스 전용 JWT 토큰 및 회원 정보를 수신합니다.<br/>"
                    + "<i>※ 주의: 임시 교환 코드는 2분간 유효하며, 1회 사용 후 즉시 파기됩니다.</i>"
    )
    @PostMapping("/token/exchange")
    public ApiResponse<AuthResponseDTO.LoginResponse> exchangeToken(
            @RequestBody @Valid AuthRequestDTO.TokenExchangeRequest request
    ) {
        AuthResponseDTO.LoginResponse response = authService.exchangeTempCode(request.code());
        return ApiResponse.onSuccess(response);
    }

    @SecurityRequirements
    @Operation(
            summary = "소셜 Access Token 로그인 / 회원가입 POST API (REST / 모바일 SDK용)",
            description = "모바일 앱 또는 프론트엔드에서 카카오/구글 SDK 등을 통해 발급받은 소셜 Access Token을 JSON으로 받아 서비스 전용 JWT 토큰을 발급합니다.<br/>"
                    + "<i>※ 웹 OAuth 브라우저 리다이렉트 흐름인 경우 POST /api/auth/token/exchange API를 사용하세요.</i>"
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
                request.accessToken(),
                deviceInfo
        );
        return ApiResponse.onSuccess(response);
    }

    @SecurityRequirements
    @Operation(
            summary = "토큰 재발급 API",
            description = "만료되었거나 만료 임박한 Access Token을 가지고 있는 사용자가 저장된 유효한 Refresh Token을 사용하여 새로운 Access Token 및 Refresh Token 세션을 재발급받습니다."
    )
    @PostMapping("/reissue")
    public ApiResponse<AuthResponseDTO.TokenInfo> reissue(
            @RequestBody @Valid AuthRequestDTO.TokenRefreshRequest request
    ) {
        AuthResponseDTO.TokenInfo tokenInfo = authService.reissueToken(request.refreshToken());
        return ApiResponse.onSuccess(tokenInfo);
    }

    @Operation(
            summary = "소셜 계정 추가 연동 API (인증 필요)",
            description = "현재 로그인된 사용자의 계정(Header의 JWT AccessToken 필요)에 새로운 소셜 계정(카카오/구글)을 추가로 연동합니다.<br/>"
                    + "- 연동하려는 소셜 계정이 이미 타 사용자에 연동되어 있는 경우 AUTH_409_01 (계정 중복 연동) 에러가 발생합니다."
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
                request.accessToken(),
                deviceInfo
        );
        return ApiResponse.onSuccess(tokenInfo);
    }

    @Operation(
            summary = "로그아웃 API (인증 필요)",
            description = "현재 로그인된 사용자(Header의 JWT AccessToken 필요)의 요청 기기 세션에 해당되는 Refresh Token을 DB에서 만료/삭제 처리합니다."
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
            summary = "회원 탈퇴 API (인증 필요)",
            description = "현재 로그인된 사용자의 계정을 탈퇴 처리하고, 연결된 모든 소셜 인증 정보(SocialAuth) 및 Refresh Token 세션을 DB에서 완전히 삭제합니다."
    )
    @PostMapping("/withdraw")
    public ApiResponse<Void> withdraw() {
        Long userId = SecurityUtil.getCurrentUserId();
        authService.withdraw(userId);
        return ApiResponse.onSuccess(null);
    }
}