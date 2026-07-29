package com.mr.domain.auth.service;

import com.mr.domain.auth.dto.AuthResponseDTO;
import com.mr.domain.auth.dto.OAuthUserInfo;
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
    private final OAuthClientService oAuthClientService;

    @Transactional
    public AuthResponseDTO.LoginResponse socialLogin(SocialType socialType, String codeOrToken, String redirectUri) {
        OAuthUserInfo userInfo = oAuthClientService.getUserInfo(socialType, codeOrToken, redirectUri);

        Long mockUserId = 1L;
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