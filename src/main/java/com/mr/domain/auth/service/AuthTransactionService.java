package com.mr.domain.auth.service;

import com.mr.domain.auth.dto.OAuthUserInfo;
import com.mr.domain.auth.dto.res.AuthResponseDTO;
import com.mr.domain.auth.entity.SocialAuth;
import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.domain.auth.exception.AuthErrorStatus;
import com.mr.domain.auth.repository.SocialAuthRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthTransactionService {

    private final SocialAuthRepository socialAuthRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    @Value("${app.profile.default-image-url}")
    private String defaultProfileImageUrl;

    public record SocialLoginPrepareResult(Long userId, String socialId, boolean isNewUser) {}

    public SocialLoginPrepareResult prepareSocialLogin(SocialType socialType, OAuthUserInfo userInfo) {
        Optional<SocialAuth> optionalSocialAuth = socialAuthRepository.findBySocialTypeAndSocialId(socialType, userInfo.socialId());
        boolean isNewUser = optionalSocialAuth.isEmpty();

        User user;
        if (optionalSocialAuth.isPresent()) {
            user = optionalSocialAuth.get().getUser();
        } else {
            user = registerNewUser(userInfo);
        }

        return new SocialLoginPrepareResult(user.getUserId(), userInfo.socialId(), isNewUser);
    }

    public AuthResponseDTO.LoginResponse completeTokenExchange(Long userId, SocialType socialType, String socialId, String deviceInfo, boolean isNewUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        String newAccessToken = tokenProvider.createAccessToken(user.getUserId());
        String newRefreshToken = tokenProvider.createRefreshToken(user.getUserId());
        String refreshTokenHash = tokenProvider.hashToken(newRefreshToken);
        LocalDateTime expiryTime = tokenProvider.getRefreshTokenExpiryTime();

        Optional<SocialAuth> optionalSocialAuth = socialAuthRepository.findBySocialTypeAndSocialId(socialType, socialId);
        if (optionalSocialAuth.isPresent()) {
            SocialAuth socialAuth = optionalSocialAuth.get();
            socialAuth.updateRefreshToken(refreshTokenHash, expiryTime, deviceInfo);
        } else {
            SocialAuth newSocialAuth = SocialAuth.create(
                    user,
                    socialType,
                    socialId,
                    refreshTokenHash,
                    expiryTime,
                    deviceInfo
            );
            socialAuthRepository.saveAndFlush(newSocialAuth);
        }

        AuthResponseDTO.TokenResponse tokenResponse = AuthResponseDTO.TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .accessTokenExpiresInSeconds(tokenProvider.getAccessTokenExpirationSeconds())
                .build();

        return AuthResponseDTO.LoginResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .isNewUser(isNewUser)
                .isOnboardingCompleted(user.isOnboardingCompleted())
                .tokenInfo(tokenResponse)
                .build();
    }

    public AuthResponseDTO.LoginResponse executeSocialLogin(SocialType socialType, OAuthUserInfo userInfo, String deviceInfo) {
        SocialLoginPrepareResult prepareResult = prepareSocialLogin(socialType, userInfo);
        return completeTokenExchange(prepareResult.userId(), socialType, prepareResult.socialId(), deviceInfo, prepareResult.isNewUser());
    }

    public AuthResponseDTO.LoginResponse executeSocialLoginForExistingUser(SocialType socialType, OAuthUserInfo userInfo, String deviceInfo) {
        SocialAuth socialAuth = socialAuthRepository.findBySocialTypeAndSocialId(socialType, userInfo.socialId())
                .orElseThrow(() -> new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST));

        User user = socialAuth.getUser();

        String newAccessToken = tokenProvider.createAccessToken(user.getUserId());
        String newRefreshToken = tokenProvider.createRefreshToken(user.getUserId());
        String refreshTokenHash = tokenProvider.hashToken(newRefreshToken);
        LocalDateTime expiryTime = tokenProvider.getRefreshTokenExpiryTime();

        socialAuth.updateRefreshToken(refreshTokenHash, expiryTime, deviceInfo);

        AuthResponseDTO.TokenResponse tokenResponse = AuthResponseDTO.TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .accessTokenExpiresInSeconds(tokenProvider.getAccessTokenExpirationSeconds())
                .build();

        return AuthResponseDTO.LoginResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .isNewUser(false)
                .isOnboardingCompleted(user.isOnboardingCompleted())
                .tokenInfo(tokenResponse)
                .build();
    }

    public AuthResponseDTO.TokenInfo executeLinkSocialAccount(Long userId, SocialType socialType, OAuthUserInfo userInfo, String deviceInfo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        Optional<SocialAuth> existingSocialAuth = socialAuthRepository.findBySocialTypeAndSocialId(socialType, userInfo.socialId());
        if (existingSocialAuth.isPresent()) {
            SocialAuth socialAuth = existingSocialAuth.get();
            if (!socialAuth.getUser().getUserId().equals(userId)) {
                throw new GeneralException(AuthErrorStatus.ALREADY_LINKED_SOCIAL_ACCOUNT);
            }
        }

        Optional<SocialAuth> userSocialAuthForType = socialAuthRepository.findAllByUser_UserId(userId).stream()
                .filter(sa -> sa.getSocialType() == socialType)
                .findFirst();

        if (userSocialAuthForType.isPresent() && !userSocialAuthForType.get().getSocialId().equals(userInfo.socialId())) {
            throw new GeneralException(AuthErrorStatus.ALREADY_LINKED_SOCIAL_ACCOUNT);
        }

        String newAccessToken = tokenProvider.createAccessToken(userId);
        String newRefreshToken = tokenProvider.createRefreshToken(userId);
        String refreshTokenHash = tokenProvider.hashToken(newRefreshToken);
        LocalDateTime expiryTime = tokenProvider.getRefreshTokenExpiryTime();

        if (existingSocialAuth.isPresent()) {
            SocialAuth socialAuth = existingSocialAuth.get();
            socialAuth.updateRefreshToken(refreshTokenHash, expiryTime, deviceInfo);
        } else {
            SocialAuth newSocialAuth = SocialAuth.create(
                    user,
                    socialType,
                    userInfo.socialId(),
                    refreshTokenHash,
                    expiryTime,
                    deviceInfo
            );
            socialAuthRepository.save(newSocialAuth);
        }

        return AuthResponseDTO.TokenInfo.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .accessTokenExpiresInSeconds(tokenProvider.getAccessTokenExpirationSeconds())
                .build();
    }

    private User registerNewUser(OAuthUserInfo userInfo) {
        String profileImgUrl = userInfo.profileImgUrl();
        if (profileImgUrl == null || profileImgUrl.isBlank()) {
            profileImgUrl = defaultProfileImageUrl;
        }

        User user = User.createFromOAuth(profileImgUrl);
        return userRepository.save(user);
    }
}
