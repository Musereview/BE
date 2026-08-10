package com.mr.domain.auth.controller;

import com.mr.domain.auth.dto.req.AuthRequestDTO;
import com.mr.domain.auth.dto.res.AuthResponseDTO;
import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.domain.auth.service.AuthService;
import com.mr.domain.auth.service.OAuthClientService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "인증", description = "소셜 로그인, JWT 토큰 발급/재발급, 계정 연동/로그아웃/회원탈퇴 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OAuthClientService oAuthClientService;

    @org.springframework.beans.factory.annotation.Value("${oauth.cookie.secure:false}")
    private boolean cookieSecure;

    private String getStateCookieName(SocialType socialType) {
        return "oauth_state_" + (socialType != null ? socialType.name().toLowerCase() : "default");
    }

    private String getRedirectCookieName(SocialType socialType) {
        return "oauth_redirect_uri_" + (socialType != null ? socialType.name().toLowerCase() : "default");
    }

    @SecurityRequirements
    @Operation(
            summary = "소셜 로그인 시작 (웹 브라우저)",
            description = "웹 브라우저 환경에서 소셜 로그인(카카오/구글) 인증 페이지로 302 리다이렉트합니다.<br/>"
                    + "<b>입력값:</b><br/>"
                    + "- <code>socialType</code> (Path, 필수): 소셜 로그인 제공자 (KAKAO, GOOGLE)<br/>"
                    + "- <code>redirectUri</code> (Query, 선택): 로그인 완료 후 이동할 프론트엔드 콜백 URL (미입력 시 기본 설정된 콜백 URL 사용)<br/>"
                    + "<b>동작 방식:</b> CSRF 방지용 state 토큰과 리다이렉트 대상을 HttpOnly 쿠키에 저장 후 소셜 인가 URL로 브라우저를 이동시킵니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302", description = "소셜 로그인 인가 URL로 302 리다이렉트"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (지원하지 않는 소셜 타입)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "COMMON_400_01",
                                    summary = "잘못된 소셜 타입 파라미터",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON_400_01",
                                      "message": "입력값이 올바르지 않습니다.",
                                      "data": { "socialType": "요청 파라미터 형식이 올바르지 않습니다." }
                                    }
                                    """
                            )
                    )
            )
    })
    @GetMapping("/login/{socialType}")
    public void startOAuthLogin(
            @Parameter(description = "소셜 로그인 제공자 (KAKAO, GOOGLE)", example = "KAKAO")
            @PathVariable(name = "socialType") SocialType socialType,
            @Parameter(description = "커스텀 프론트엔드 리다이렉트 URI (선택)", example = "https://www.musereview.site/oauth/callback")
            @RequestParam(name = "redirectUri", required = false) String customRedirectUri,
            HttpServletResponse response
    ) throws IOException {
        String state = UUID.randomUUID().toString();

        ResponseCookie stateCookie = ResponseCookie.from(getStateCookieName(socialType), state)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(300)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, stateCookie.toString());

        if (customRedirectUri != null && !customRedirectUri.isBlank()) {
            String trimmedUri = customRedirectUri.trim();
            if (oAuthClientService.isBackendAllowedRedirectUri(socialType, trimmedUri)
                    || oAuthClientService.isFrontendAllowedRedirectUri(trimmedUri)) {
                ResponseCookie redirectCookie = ResponseCookie.from(getRedirectCookieName(socialType), trimmedUri)
                        .httpOnly(true)
                        .secure(cookieSecure)
                        .path("/")
                        .maxAge(300)
                        .sameSite("Lax")
                        .build();
                response.addHeader(HttpHeaders.SET_COOKIE, redirectCookie.toString());
            } else {
                log.warn("{} custom redirectUri [{}] is not in allowed backend/frontend list. Ignoring.", socialType, trimmedUri);
            }
        }

        String authUrl = oAuthClientService.getAuthorizationUrl(socialType, customRedirectUri, state);
        response.sendRedirect(authUrl);
    }

    @SecurityRequirements
    @Operation(
            summary = "OAuth 소셜 로그인 콜백 수신",
            description = "소셜 로그인 제공자(카카오/구글)에서 인증 완료 후 인가 코드를 전달받는 백엔드 콜백 엔드포인트입니다.<br/>"
                    + "<b>입력값:</b><br/>"
                    + "- <code>socialType</code> (Path, 필수): 소셜 로그인 제공자 (KAKAO, GOOGLE)<br/>"
                    + "- <code>code</code> (Query, 필수): 소셜 제공자가 발급한 인가 코드(Authorization Code)<br/>"
                    + "- <code>state</code> (Query, 필수): CSRF 검증용 state 값 (로그인 시작 시 발급된 쿠키와 일치해야 함)<br/>"
                    + "- <code>error</code> (Query, 선택): 사용자 취소 또는 소셜 제공자 측 오류 발생 시 전달되는 에러 코드<br/>"
                    + "- <code>User-Agent</code> (Header, 선택): 접속 기기 정보 (미입력 시 'Unknown Device')<br/>"
                    + "<b>동작 방식:</b> 인가 코드로 1회성 임시 교환 코드(tempCode, 2분 유효)를 생성한 뒤, 프론트엔드 콜백 페이지로 302 리다이렉트합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302", description = "프론트엔드 콜백 페이지로 302 리다이렉트 (?code={tempCode} 또는 ?error={errorCode})")
    })
    @GetMapping("/{socialType}/callback")
    public void oAuthCallback(
            @Parameter(description = "소셜 로그인 제공자 (KAKAO, GOOGLE)", example = "KAKAO")
            @PathVariable(name = "socialType") SocialType socialType,
            @Parameter(description = "소셜 인가 코드", example = "sample_authorization_code")
            @RequestParam(name = "code", required = false) String code,
            @Parameter(description = "CSRF 검증용 state 값", example = "sample_state_uuid")
            @RequestParam(name = "state", required = false) String state,
            @Parameter(description = "소셜 로그인 에러 코드 (실패 시)", example = "access_denied")
            @RequestParam(name = "error", required = false) String error,
            @Parameter(description = "커스텀 리다이렉트 URI (선택)", example = "https://www.musereview.site/oauth/callback")
            @RequestParam(name = "redirectUri", required = false) String customRedirectUri,
            @RequestHeader(value = HttpHeaders.USER_AGENT, defaultValue = "Unknown Device") String deviceInfo,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        String savedState = null;
        String savedCustomRedirectUri = null;
        String stateCookieName = getStateCookieName(socialType);
        String redirectCookieName = getRedirectCookieName(socialType);

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (stateCookieName.equals(cookie.getName())) {
                    savedState = cookie.getValue();
                } else if (redirectCookieName.equals(cookie.getName())) {
                    savedCustomRedirectUri = cookie.getValue();
                }
            }
        }

        ResponseCookie clearStateCookie = ResponseCookie.from(stateCookieName, "")
                .httpOnly(true).secure(cookieSecure).path("/").maxAge(0).sameSite("Lax").build();
        ResponseCookie clearRedirectCookie = ResponseCookie.from(redirectCookieName, "")
                .httpOnly(true).secure(cookieSecure).path("/").maxAge(0).sameSite("Lax").build();
        response.addHeader(HttpHeaders.SET_COOKIE, clearStateCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearRedirectCookie.toString());

        String effectiveRedirectUri = (savedCustomRedirectUri != null && !savedCustomRedirectUri.isBlank())
                ? savedCustomRedirectUri
                : customRedirectUri;

        String backendOAuthRedirectUri = null;
        String frontendTargetRedirectUri = null;

        if (effectiveRedirectUri != null && !effectiveRedirectUri.isBlank()) {
            String trimmedUri = effectiveRedirectUri.trim();
            if (oAuthClientService.isBackendAllowedRedirectUri(socialType, trimmedUri)) {
                backendOAuthRedirectUri = trimmedUri;
            } else if (oAuthClientService.isFrontendAllowedRedirectUri(trimmedUri)) {
                frontendTargetRedirectUri = trimmedUri;
            } else {
                log.warn("{} effective redirectUri [{}] is not allowed for backend or frontend. Falling back to defaults.", socialType, trimmedUri);
            }
        }

        if (savedState == null) {
            log.warn("{} OAuth state cookie missing in callback request", socialType);
            String errorRedirectUrl = oAuthClientService.buildFrontendErrorRedirectUrl("invalid_auth_request", frontendTargetRedirectUri);
            response.sendRedirect(errorRedirectUrl);
            return;
        }

        if (state == null || !savedState.equals(state)) {
            log.warn("{} OAuth state mismatch detected in callback request", socialType);
            String errorRedirectUrl = oAuthClientService.buildFrontendErrorRedirectUrl("invalid_state", frontendTargetRedirectUri);
            response.sendRedirect(errorRedirectUrl);
            return;
        }

        if (error != null || code == null || code.isBlank()) {
            log.warn("{} OAuth login failed/cancelled by user: error={}", socialType, error);
            String errorCode = "access_denied".equalsIgnoreCase(error) ? "access_denied" : "oauth_failed";
            String errorRedirectUrl = oAuthClientService.buildFrontendErrorRedirectUrl(errorCode, frontendTargetRedirectUri);
            response.sendRedirect(errorRedirectUrl);
            return;
        }

        try {
            String tempCode = authService.generateTempCodeByCode(
                    socialType,
                    code,
                    backendOAuthRedirectUri,
                    deviceInfo
            );
            String targetFrontendUrl = oAuthClientService.buildFrontendRedirectUrl(tempCode, frontendTargetRedirectUri);
            response.sendRedirect(targetFrontendUrl);
        } catch (Exception e) {
            log.error("{} OAuth callback processing failed: {}", socialType, e.getMessage(), e);
            String errorRedirectUrl = oAuthClientService.buildFrontendErrorRedirectUrl("authentication_failed", frontendTargetRedirectUri);
            response.sendRedirect(errorRedirectUrl);
        }
    }

    @SecurityRequirements
    @Operation(
            summary = "임시 교환 코드로 JWT 토큰 발급",
            description = "소셜 로그인 콜백에서 수신한 1회성 임시 교환 코드(code)를 전달하여 서비스 전용 JWT 토큰(Access Token, Refresh Token) 및 유저 기본 정보를 발급받습니다.<br/>"
                    + "<b>입력값:</b><br/>"
                    + "- <code>code</code> (Body, 필수): 콜백 URL 쿼리 파라미터로 수신한 1회성 임시 교환 코드<br/>"
                    + "<b>주의사항:</b> 임시 교환 코드는 발급 후 2분간 유효하며, 1회 교환 즉시 파기됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "토큰 발급 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "SUCCESS",
                                    summary = "토큰 발급 성공 예시",
                                    value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON_200",
                                      "message": "요청에 성공하였습니다.",
                                      "data": {
                                        "userId": 1,
                                        "isProfileRegistered": true,
                                        "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZSI6IlJPTEVfU1RVREVOVCIsImV4cCI6MTY5...",
                                        "refreshToken": "d9f8e7c6-b5a4-3210-9876-fedcba098765"
                                      }
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 또는 만료된 임시 코드",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "COMMON_400_01",
                                            summary = "필수값 누락 (code 빈값)",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "COMMON_400_01",
                                              "message": "입력값이 올바르지 않습니다.",
                                              "data": { "code": "임시 교환 코드는 필수 입력값입니다." }
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "AUTH_400_01",
                                            summary = "유효하지 않거나 만료된 임시 코드",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "AUTH_400_01",
                                              "message": "인증 관련 필수 입력값이 올바르지 않거나 누락되었습니다.",
                                              "data": null
                                            }
                                            """
                                    )
                            }
                    )
            )
    })
    @PostMapping("/token/exchange")
    public ApiResponse<AuthResponseDTO.LoginResponse> exchangeToken(
            @RequestBody @Valid AuthRequestDTO.TokenExchangeRequest request
    ) {
        AuthResponseDTO.LoginResponse response = authService.exchangeTempCode(request.code());
        return ApiResponse.onSuccess(response);
    }

    @SecurityRequirements
    @Operation(
            summary = "소셜 Access Token 로그인 / 회원가입 (REST / 모바일 SDK용)",
            description = "모바일 앱 또는 프론트엔드 SDK에서 직접 발급받은 소셜 Access Token을 전달받아 회원가입 및 로그인을 처리하고 JWT 토큰을 발급합니다.<br/>"
                    + "<b>입력값:</b><br/>"
                    + "- <code>socialType</code> (Path, 필수): 소셜 로그인 제공자 (KAKAO, GOOGLE)<br/>"
                    + "- <code>accessToken</code> (Body, 필수): 클라이언트 SDK에서 발급받은 소셜 Access Token<br/>"
                    + "- <code>User-Agent</code> (Header, 선택): 접속 기기 식별 정보<br/>"
                    + "<b>참고:</b> 웹 브라우저 OAuth 리다이렉트 흐름의 경우 <code>GET /api/auth/login/{socialType}</code> 및 <code>POST /api/auth/token/exchange</code>를 사용하세요."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 / 회원가입 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "SUCCESS",
                                    summary = "로그인 성공 예시",
                                    value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON_200",
                                      "message": "요청에 성공하였습니다.",
                                      "data": {
                                        "userId": 1,
                                        "isProfileRegistered": false,
                                        "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZSI6IlJPTEVfR1VFU1QiLCJleHAiOjE2OS...",
                                        "refreshToken": "d9f8e7c6-b5a4-3210-9876-fedcba098765"
                                      }
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 파라미터",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "COMMON_400_01",
                                    summary = "Access Token 누락",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON_400_01",
                                      "message": "입력값이 올바르지 않습니다.",
                                      "data": { "accessToken": "Access Token은 필수 입력값입니다." }
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "소셜 토큰 인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "AUTH_401_04",
                                    summary = "유효하지 않은 소셜 토큰",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "AUTH_401_04",
                                      "message": "소셜 로그인 인증에 실패했거나 유효하지 않은 소셜 액세스 토큰입니다.",
                                      "data": null
                                    }
                                    """
                            )
                    )
            )
    })
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
            summary = "JWT 토큰 재발급",
            description = "Access Token 만료 시, 유효한 Refresh Token을 검증하여 새로운 Access Token 및 Refresh Token 세션을 재발급합니다.<br/>"
                    + "<b>입력값:</b><br/>"
                    + "- <code>refreshToken</code> (Body, 필수): 클라이언트에 저장된 유효한 Refresh Token<br/>"
                    + "<b>주의사항:</b> Refresh Token Rotation(RTR)이 적용되어 기존 Refresh Token은 즉시 무효화되고 새 토큰 세션이 생성됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "토큰 재발급 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "SUCCESS",
                                    summary = "재발급 성공 예시",
                                    value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON_200",
                                      "message": "요청에 성공하였습니다.",
                                      "data": {
                                        "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZSI6IlJPTEVfU1RVREVOVCIsImV4cCI6MTY5...",
                                        "refreshToken": "e8d7c6b5-a4b3-2109-8765-fedcba098765"
                                      }
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 파라미터",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "COMMON_400_01",
                                    summary = "Refresh Token 누락",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON_400_01",
                                      "message": "입력값이 올바르지 않습니다.",
                                      "data": { "refreshToken": "Refresh Token은 필수 입력값입니다." }
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (유효하지 않거나 만료된 Refresh Token)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "AUTH_401_01",
                                            summary = "유효하지 않은 토큰",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "AUTH_401_01",
                                              "message": "유효하지 않은 토큰입니다.",
                                              "data": null
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "AUTH_401_02",
                                            summary = "만료된 토큰",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "AUTH_401_02",
                                              "message": "만료된 토큰입니다.",
                                              "data": null
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "AUTH_401_03",
                                            summary = "폐기/로그아웃된 토큰",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "AUTH_401_03",
                                              "message": "명시적으로 폐기 및 로그아웃 처리된 토큰입니다.",
                                              "data": null
                                            }
                                            """
                                    )
                            }
                    )
            )
    })
    @PostMapping("/reissue")
    public ApiResponse<AuthResponseDTO.TokenInfo> reissue(
            @RequestBody @Valid AuthRequestDTO.TokenRefreshRequest request
    ) {
        AuthResponseDTO.TokenInfo tokenInfo = authService.reissueToken(request.refreshToken());
        return ApiResponse.onSuccess(tokenInfo);
    }

    @Operation(
            summary = "소셜 계정 추가 연동",
            description = "현재 로그인된 사용자 계정에 새로운 소셜 계정(카카오/구글)을 추가로 연동합니다.<br/>"
                    + "<b>입력값:</b><br/>"
                    + "- <code>Authorization</code> (Header, 필수): Bearer 형태의 현재 로그인 유저 JWT Access Token<br/>"
                    + "- <code>socialType</code> (Path, 필수): 추가 연동할 소셜 제공자 (KAKAO, GOOGLE)<br/>"
                    + "- <code>accessToken</code> (Body, 필수): 연동하려는 소셜 계정의 유효한 Access Token<br/>"
                    + "- <code>User-Agent</code> (Header, 선택): 접속 기기 식별 정보<br/>"
                    + "<b>주의사항:</b> 연동하려는 소셜 계정이 이미 다른 유저 계정에 연결되어 있는 경우 <code>409 Conflict (AUTH_409_01)</code> 에러가 발생합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "소셜 계정 연동 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "SUCCESS",
                                    summary = "연동 성공 예시",
                                    value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON_200",
                                      "message": "요청에 성공하였습니다.",
                                      "data": {
                                        "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZSI6IlJPTEVfU1RVREVOVCIsImV4cCI6MTY5...",
                                        "refreshToken": "d9f8e7c6-b5a4-3210-9876-fedcba098765"
                                      }
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 파라미터",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "COMMON_400_01",
                                    summary = "Access Token 누락",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON_400_01",
                                      "message": "입력값이 올바르지 않습니다.",
                                      "data": { "accessToken": "Access Token은 필수 입력값입니다." }
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (로그인 토큰 누락 또는 소셜 토큰 유효성 오류)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "COMMON_401_01",
                                            summary = "로그인 인증 토큰 누락/만료",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "COMMON_401_01",
                                              "message": "인증이 필요합니다.",
                                              "data": null
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "AUTH_401_04",
                                            summary = "유효하지 않은 소셜 Access Token",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "AUTH_401_04",
                                              "message": "소셜 로그인 인증에 실패했거나 유효하지 않은 소셜 액세스 토큰입니다.",
                                              "data": null
                                            }
                                            """
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "계정 연동 충돌 (이미 타 계정에 연동됨)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "AUTH_409_01",
                                    summary = "이미 다른 계정에 연동된 소셜 계정",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "AUTH_409_01",
                                      "message": "이미 다른 계정에 연동되어 있는 소셜 계정입니다.",
                                      "data": null
                                    }
                                    """
                            )
                    )
            )
    })
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
            summary = "로그아웃",
            description = "현재 로그인된 사용자의 접속 세션을 종료하고, DB에 저장된 해당 기기의 Refresh Token을 만료/삭제 처리합니다.<br/>"
                    + "<b>입력값:</b><br/>"
                    + "- <code>Authorization</code> (Header, 필수): Bearer 형태의 현재 로그인 유저 JWT Access Token<br/>"
                    + "- <code>refreshToken</code> (Body, 필수): 로그아웃할 기기 세션의 Refresh Token"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "SUCCESS",
                                    summary = "로그아웃 성공 예시",
                                    value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON_200",
                                      "message": "요청에 성공하였습니다.",
                                      "data": null
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 파라미터",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "COMMON_400_01",
                                    summary = "Refresh Token 누락",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON_400_01",
                                      "message": "입력값이 올바르지 않습니다.",
                                      "data": { "refreshToken": "Refresh Token은 필수 입력값입니다." }
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (로그인 토큰 누락)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "COMMON_401_01",
                                    summary = "인증 토큰 누락/만료",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON_401_01",
                                      "message": "인증이 필요합니다.",
                                      "data": null
                                    }
                                    """
                            )
                    )
            )
    })
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestBody @Valid AuthRequestDTO.LogoutRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        authService.logout(userId, request.refreshToken());
        return ApiResponse.onSuccess(null);
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인된 사용자의 회원 탈퇴를 처리하고, 연결된 모든 소셜 인증 정보(SocialAuth) 및 활성화된 Refresh Token 세션을 완전히 삭제합니다.<br/>"
                    + "<b>입력값:</b><br/>"
                    + "- <code>Authorization</code> (Header, 필수): Bearer 형태의 현재 로그인 유저 JWT Access Token<br/>"
                    + "<b>주의사항:</b> 탈퇴 처리 시 모든 인증 세션이 즉시 만료되며, 기존 연동 데이터는 복구할 수 없습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "회원 탈퇴 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "SUCCESS",
                                    summary = "회원 탈퇴 성공 예시",
                                    value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON_200",
                                      "message": "요청에 성공하였습니다.",
                                      "data": null
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (로그인 토큰 누락)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "COMMON_401_01",
                                    summary = "인증 토큰 누락/만료",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON_401_01",
                                      "message": "인증이 필요합니다.",
                                      "data": null
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자 또는 소셜 인증 정보 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "AUTH_404_01",
                                    summary = "소셜 인증 기록 없음",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "AUTH_404_01",
                                      "message": "해당 사용자의 소셜 인증 기록을 찾을 수 없습니다.",
                                      "data": null
                                    }
                                    """
                            )
                    )
            )
    })
    @PostMapping("/withdraw")
    public ApiResponse<Void> withdraw() {
        Long userId = SecurityUtil.getCurrentUserId();
        authService.withdraw(userId);
        return ApiResponse.onSuccess(null);
    }
}