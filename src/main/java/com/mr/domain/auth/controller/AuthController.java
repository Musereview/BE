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
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
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
        String state = UUID.randomUUID().toString();

        ResponseCookie stateCookie = ResponseCookie.from("oauth_state", state)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofMinutes(5))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, stateCookie.toString());

        if (customRedirectUri != null && !customRedirectUri.isBlank()) {
            ResponseCookie redirectCookie = ResponseCookie.from("oauth_redirect_uri", customRedirectUri.trim())
                    .httpOnly(true)
                    .path("/")
                    .maxAge(Duration.ofMinutes(5))
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, redirectCookie.toString());
        }

        String authUrl = oAuthClientService.getAuthorizationUrl(socialType, customRedirectUri, state);
        response.sendRedirect(authUrl);
    }

    @SecurityRequirements
    @Operation(
            summary = "소셜 로그인 백엔드 콜백 API (OAuth Server ➔ Backend)",
            description = "OAuth 제공자로부터 인가 코드(code)를 받아 토큰 교환 및 소셜 로그인을 수행한 후 프론트엔드 페이지로 리다이렉트합니다.<br/>"
                    + "- 소셜 인증 취소, state 미일치 또는 인증 실패 시에도 프론트엔드 페이지로 error 쿼리 파라미터와 함께 리다이렉트합니다."
    )
    @GetMapping("/{socialType}/callback")
    public void oAuthCallback(
            @Parameter(description = "소셜 로그인 제공자 (KAKAO, GOOGLE)", example = "KAKAO")
            @PathVariable(name = "socialType") SocialType socialType,
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "error", required = false) String error,
            @CookieValue(name = "oauth_state", required = false) String cookieState,
            @CookieValue(name = "oauth_redirect_uri", required = false) String savedCustomRedirectUri,
            @RequestParam(name = "redirectUri", required = false) String customRedirectUri,
            @RequestHeader(value = HttpHeaders.USER_AGENT, defaultValue = "Unknown Device") String deviceInfo,
            HttpServletResponse response
    ) throws IOException {
        if (state == null || cookieState == null || !state.equals(cookieState)) {
            deleteCookie(response, "oauth_state");
            deleteCookie(response, "oauth_redirect_uri");
            String errorRedirectUrl = oAuthClientService.buildFrontendErrorRedirectUrl("invalid_state");
            response.sendRedirect(errorRedirectUrl);
            return;
        }

        deleteCookie(response, "oauth_state");
        deleteCookie(response, "oauth_redirect_uri");

        if (error != null && !error.isBlank()) {
            String errorRedirectUrl = oAuthClientService.buildFrontendErrorRedirectUrl(
                    error.equalsIgnoreCase("access_denied") ? "access_denied" : "oauth_error"
            );
            response.sendRedirect(errorRedirectUrl);
            return;
        }

        if (code == null || code.isBlank()) {
            String errorRedirectUrl = oAuthClientService.buildFrontendErrorRedirectUrl("invalid_auth_request");
            response.sendRedirect(errorRedirectUrl);
            return;
        }

        String effectiveRedirectUri = (savedCustomRedirectUri != null && !savedCustomRedirectUri.isBlank())
                ? savedCustomRedirectUri
                : customRedirectUri;

        try {
            OAuthCredential credential = new OAuthCredential(OAuthCredential.CredentialType.AUTHORIZATION_CODE, code);
            AuthResponseDTO.LoginResponse loginResponse = authService.socialLogin(
                    socialType,
                    credential,
                    effectiveRedirectUri,
                    deviceInfo
            );

            String tempCode = authService.generateTempExchangeCode(loginResponse);
            String targetFrontendUrl = oAuthClientService.buildFrontendRedirectUrl(tempCode);
            response.sendRedirect(targetFrontendUrl);
        } catch (Exception e) {
            String errorCode = (e instanceof GeneralException ge && ge.getCode() != null)
                    ? ge.getCode().getCode()
                    : "authentication_failed";
            String errorRedirectUrl = oAuthClientService.buildFrontendErrorRedirectUrl(errorCode);
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
            summary = "소셜 로그인 / 회원가입 POST API (REST / JSON 요청용)",
            description = "프론트엔드/모바일에서 소셜 인가 코드(code 또는 authorizationCode) 또는 Access Token을 JSON으로 받아 서비스 전용 JWT 토큰을 발급합니다.<br/>"
                    + "- <b>code / authorizationCode / accessToken</b>: 셋 중 전송된 하나를 백엔드가 자동 감지하여 인가 처리합니다.<br/>"
                    + "- <b>redirectUri</b>: 생략 시 백엔드 .env/yml 기본값이 자동 적용됩니다.<br/>"
                    + "<i>※ 주의: OAuth 2.0 인가 코드는 1회용이므로, 이미 사용된 코드로 재요청 시 AUTH_401_04 오류가 발생합니다.</i>"
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
                request.getCredential(),
                request.redirectUri(),
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