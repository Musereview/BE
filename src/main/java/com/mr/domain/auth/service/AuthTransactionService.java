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
import org.springframework.dao.DataIntegrityViolationException;
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

    public record SocialLoginPrepareResult(Long userId, String socialId, String profileImgUrl, boolean isNewUser) {}

    @Transactional(readOnly = true)
    public SocialLoginPrepareResult prepareSocialLogin(SocialType socialType, OAuthUserInfo userInfo) {
        Optional<SocialAuth> optionalSocialAuth = socialAuthRepository.findBySocialTypeAndSocialId(socialType, userInfo.socialId());
        if (optionalSocialAuth.isPresent()) {
            User user = optionalSocialAuth.get().getUser();
            return new SocialLoginPrepareResult(user.getUserId(), userInfo.socialId(), userInfo.profileImgUrl(), false);
        } else {
            return new SocialLoginPrepareResult(null, userInfo.socialId(), userInfo.profileImgUrl(), true);
        }
    }

    public AuthResponseDTO.LoginResponse completeTokenExchange(Long userId, SocialType socialType, String socialId, String profileImgUrl, String deviceInfo, boolean isNewUser) {
        User user;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));
        } else {
            user = registerNewUserByProfileUrl(profileImgUrl);
        }

        String newAccessToken = tokenProvider.createAccessToken(user.getUserId());
        String newRefreshToken = tokenProvider.createRefreshToken(user.getUserId());
        String refreshTokenHash = tokenProvider.hashToken(newRefreshToken);
        LocalDateTime expiryTime = tokenProvider.getRefreshTokenExpiryTime();

        // PostgreSQL은 제약 위반이 나는 즉시 트랜잭션 전체를 abort 상태로 만들어서,
        // 같은 트랜잭션 안에서 복구 쿼리를 다시 시도하면 그 쿼리도 실패한다.
        // 여기서 잡아 도메인 예외로 변환하지 않고 그대로 전파해 트랜잭션을 롤백시키고,
        // 호출 쪽(AuthService의 동시 로그인 재시도 로직)이 새 트랜잭션에서 복구를 시도하게 한다.
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
        return completeTokenExchange(prepareResult.userId(), socialType, prepareResult.socialId(), prepareResult.profileImgUrl(), deviceInfo, prepareResult.isNewUser());
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

        try {
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
                socialAuthRepository.saveAndFlush(newSocialAuth);
            }
        } catch (DataIntegrityViolationException e) {
            throw new GeneralException(AuthErrorStatus.ALREADY_LINKED_SOCIAL_ACCOUNT);
        }

        return AuthResponseDTO.TokenInfo.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .accessTokenExpiresInSeconds(tokenProvider.getAccessTokenExpirationSeconds())
                .build();
    }

    private User registerNewUserByProfileUrl(String profileImgUrl) {
        if (profileImgUrl == null || profileImgUrl.isBlank()) {
            profileImgUrl = defaultProfileImageUrl;
        }

        User user = User.createFromOAuth(profileImgUrl);
        return userRepository.save(user);
    }
}
