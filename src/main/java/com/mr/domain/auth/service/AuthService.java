package com.mr.domain.auth.service;

import com.mr.domain.auth.dto.OAuthUserInfo;
import com.mr.domain.auth.dto.res.AuthResponseDTO;
import com.mr.domain.auth.entity.SocialAuth;
import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.domain.auth.exception.AuthErrorStatus;
import com.mr.domain.auth.repository.SocialAuthRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.repository.UserRepository;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final SocialAuthRepository socialAuthRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final OAuthClientService oAuthClientService;

    @Transactional
    public AuthResponseDTO.LoginResponse socialLogin(SocialType socialType, String accessToken, String deviceInfo) {
        OAuthUserInfo userInfo = oAuthClientService.getUserInfo(socialType, accessToken);

        SocialAuth socialAuth = socialAuthRepository.findBySocialTypeAndSocialId(socialType, userInfo.socialId())
                .orElseGet(() -> registerNewUser(socialType, userInfo, deviceInfo));

        User user = socialAuth.getUser();
        String newAccessToken = tokenProvider.createAccessToken(user.getUserId());
        String newRefreshToken = tokenProvider.createRefreshToken(user.getUserId());
        String refreshTokenHash = tokenProvider.hashToken(newRefreshToken);

        socialAuth.updateRefreshToken(newRefreshToken, refreshTokenHash, tokenProvider.getRefreshTokenExpiryTime(), deviceInfo);
        AuthResponseDTO.TokenResponse tokenResponse = AuthResponseDTO.TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .accessTokenExpiresInSeconds(tokenProvider.getAccessTokenExpirationSeconds())
                .build();

        boolean isNewUser = socialAuth.getCreatedAt().isAfter(java.time.LocalDateTime.now().minusMinutes(1));

        return AuthResponseDTO.LoginResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .isNewUser(isNewUser)
                .isOnboardingCompleted(user.isOnboardingCompleted())
                .tokenInfo(tokenResponse)
                .build();
    }

    private SocialAuth registerNewUser(SocialType socialType, OAuthUserInfo userInfo, String deviceInfo) {
        // 🌟 프로필 이미지가 null인 경우 기본 이미지 URL로 대체 (또는 팀 내 공통 기본 이미지 상수 사용)
        String profileImgUrl = userInfo.profileImgUrl();
        if (profileImgUrl == null || profileImgUrl.isBlank()) {
            profileImgUrl = "https://example.com/default-profile.png"; // 팀에서 쓰는 기본 이미지 URL로 변경
        }

        // 🌟 User 엔티티 생성 팩토리 메서드 호출
        User user = User.createFromOAuth(profileImgUrl);
        userRepository.save(user);

        // 초기 토큰 값 생성 후 SocialAuth 매핑
        String initialToken = tokenProvider.createRefreshToken(user.getUserId());
        String initialHash = tokenProvider.hashToken(initialToken);

        SocialAuth socialAuth = SocialAuth.create(
                user,
                socialType,
                userInfo.socialId(),
                initialToken,
                initialHash,
                tokenProvider.getRefreshTokenExpiryTime(),
                deviceInfo
        );

        return socialAuthRepository.save(socialAuth);
    }

    @Transactional
    public AuthResponseDTO.TokenInfo reissueToken(String refreshToken) {
        if (!tokenProvider.validateRefreshToken(refreshToken)) {
            throw new GeneralException(AuthErrorStatus.INVALID_TOKEN);
        }

        Long userId = Long.valueOf(tokenProvider.getAuthentication(refreshToken).getName());

        SocialAuth socialAuth = socialAuthRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST));

        String requestTokenHash = tokenProvider.hashToken(refreshToken);
        if (!requestTokenHash.equals(socialAuth.getRefreshTokenHash())) {
            throw new GeneralException(AuthErrorStatus.REVOKED_TOKEN);
        }

        String newAccessToken = tokenProvider.createAccessToken(userId);
        String newRefreshToken = tokenProvider.createRefreshToken(userId);
        String newRefreshTokenHash = tokenProvider.hashToken(newRefreshToken);

        socialAuth.updateRefreshToken(newRefreshToken, newRefreshTokenHash, tokenProvider.getRefreshTokenExpiryTime(), socialAuth.getDeviceInfo());

        return AuthResponseDTO.TokenInfo.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .accessTokenExpiresInSeconds(tokenProvider.getAccessTokenExpirationSeconds())
                .build();
    }

    @Transactional
    public void logout(Long userId) {
        SocialAuth socialAuth = socialAuthRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST));

        socialAuth.expireToken();
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        SocialAuth socialAuth = socialAuthRepository.findByUser_UserId(userId).orElse(null);
        if (socialAuth != null) {
            socialAuth.expireToken();
            socialAuthRepository.delete(socialAuth);
        }

        userRepository.delete(user);
    }
}