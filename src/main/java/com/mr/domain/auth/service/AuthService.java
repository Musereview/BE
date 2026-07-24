package com.mr.domain.auth.service;

import com.mr.domain.auth.dto.AuthResponseDTO;
import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    // private final KakaoOAuthService kakaoOAuthService; (외부 API 파싱용 서비스)
    // private final GoogleOAuthService googleOAuthService;

    @Transactional
    public AuthResponseDTO.LoginResponse socialLogin(SocialType socialType, String accessToken) {
        // 1. 외부 소셜 API (카카오/구글) 통신하여 유저 프로필(email, socialId) 파싱
        // SocialUserInfo userInfo = getSocialUserInfo(socialType, accessToken);

        // 2. TODO: User 엔티티 연동 및 가입여부 검증 (Stub 구조)
        // 만약 가입 안 되어있으면 DB User 생성 -> 저장
        Long mockUserId = 1L;
        String mockEmail = "user@example.com";
        String mockNickname = "뮤즈유저";
        boolean isNewUser = false;

        String appAccessToken = jwtTokenProvider.createAccessToken(mockUserId);
        String appRefreshToken = jwtTokenProvider.createRefreshToken(mockUserId);


        AuthResponseDTO.TokenResponse tokenResponse = AuthResponseDTO.TokenResponse.builder()
                .accessToken(appAccessToken)
                .refreshToken(appRefreshToken)
                .accessTokenExpiresInSeconds(3600L)
                .build();
        return AuthResponseDTO.LoginResponse.builder()
                .userId(mockUserId)
                .nickname(mockNickname)
                .isNewUser(isNewUser)
                .tokenInfo(tokenResponse)
                .build();
    }
}