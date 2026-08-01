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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final SocialAuthRepository socialAuthRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final OAuthClientService oAuthClientService;
    private final AuthTransactionService authTransactionService;
    private final OAuthTempCodeStore tempCodeStore;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AuthResponseDTO.LoginResponse socialLogin(SocialType socialType, String accessToken, String deviceInfo) {
        OAuthUserInfo userInfo = oAuthClientService.getUserInfo(socialType, accessToken);

        try {
            return authTransactionService.executeSocialLogin(socialType, userInfo, deviceInfo);
        } catch (DataIntegrityViolationException e) {
            return authTransactionService.executeSocialLoginForExistingUser(socialType, userInfo, deviceInfo);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AuthResponseDTO.LoginResponse socialLoginByCode(SocialType socialType, String code, String redirectUri, String deviceInfo) {
        OAuthUserInfo userInfo = oAuthClientService.getUserInfoByCode(socialType, code, redirectUri);

        try {
            return authTransactionService.executeSocialLogin(socialType, userInfo, deviceInfo);
        } catch (DataIntegrityViolationException e) {
            return authTransactionService.executeSocialLoginForExistingUser(socialType, userInfo, deviceInfo);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String generateTempCodeByCode(SocialType socialType, String code, String redirectUri, String deviceInfo) {
        OAuthUserInfo userInfo = oAuthClientService.getUserInfoByCode(socialType, code, redirectUri);
        AuthTransactionService.SocialLoginPrepareResult prepareResult = authTransactionService.prepareSocialLogin(socialType, userInfo);

        String tempCode = UUID.randomUUID().toString();
        tempCodeStore.save(tempCode, new OAuthTempCodeStore.TempExchangeData(
                prepareResult.userId(),
                socialType,
                prepareResult.socialId(),
                prepareResult.profileImgUrl(),
                deviceInfo
        ));
        return tempCode;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AuthResponseDTO.TokenInfo linkSocialAccount(Long userId, SocialType socialType, String accessToken, String deviceInfo) {
        OAuthUserInfo userInfo = oAuthClientService.getUserInfo(socialType, accessToken);
        return authTransactionService.executeLinkSocialAccount(userId, socialType, userInfo, deviceInfo);
    }

    @Transactional
    public AuthResponseDTO.TokenInfo reissueToken(String refreshToken) {
        if (!tokenProvider.validateRefreshToken(refreshToken)) {
            throw new GeneralException(AuthErrorStatus.INVALID_TOKEN);
        }

        Long userId = tokenProvider.getUserIdFromToken(refreshToken);
        String requestTokenHash = tokenProvider.hashToken(refreshToken);

        SocialAuth socialAuth = socialAuthRepository.findByRefreshTokenHashWithLock(requestTokenHash)
                .filter(auth -> auth.getUser().getUserId().equals(userId))
                .filter(auth -> !auth.isExpired())
                .orElseThrow(() -> new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST));

        String newAccessToken = tokenProvider.createAccessToken(userId);
        String newRefreshToken = tokenProvider.createRefreshToken(userId);
        String newRefreshTokenHash = tokenProvider.hashToken(newRefreshToken);

        socialAuth.updateRefreshToken(newRefreshTokenHash, tokenProvider.getRefreshTokenExpiryTime(), socialAuth.getDeviceInfo());

        return AuthResponseDTO.TokenInfo.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .accessTokenExpiresInSeconds(tokenProvider.getAccessTokenExpirationSeconds())
                .build();
    }

    @Transactional
    public void logout(Long userId, String refreshToken) {
        if (!tokenProvider.validateRefreshToken(refreshToken)) {
            throw new GeneralException(AuthErrorStatus.INVALID_TOKEN);
        }

        String requestTokenHash = tokenProvider.hashToken(refreshToken);

        SocialAuth socialAuth = socialAuthRepository.findByRefreshTokenHashWithLock(requestTokenHash)
                .filter(auth -> auth.getUser().getUserId().equals(userId))
                .orElseThrow(() -> new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST));

        socialAuth.expireToken();
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        List<SocialAuth> socialAuths = socialAuthRepository.findAllByUser_UserId(userId);
        if (!socialAuths.isEmpty()) {
            socialAuths.forEach(SocialAuth::expireToken);
            socialAuthRepository.deleteAll(socialAuths);
        }

        userRepository.delete(user);
    }

    // ⚠️ 이 어노테이션을 지우면 클래스 레벨 @Transactional(readOnly=true)를 그대로 상속받아
    // SocialAuth INSERT/UPDATE가 read-only 트랜잭션에서 실패한다 (#94 배포 서버 503 원인).
    // AuthServiceTest는 클래스 전체가 @Transactional로 감싸져 있어 이 회귀를 못 잡으니 주의.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AuthResponseDTO.LoginResponse exchangeTempCode(String tempCode) {
        if (tempCode == null || tempCode.isBlank()) {
            throw new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST);
        }
        OAuthTempCodeStore.TempExchangeData data = tempCodeStore.consume(tempCode)
                .orElseThrow(() -> new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST));

        try {
            return authTransactionService.completeTokenExchange(
                    data.userId(),
                    data.socialType(),
                    data.socialId(),
                    data.profileImgUrl(),
                    data.deviceInfo()
            );
        } catch (DataIntegrityViolationException e) {
            return authTransactionService.executeSocialLoginForExistingUser(
                    data.socialType(),
                    new OAuthUserInfo(data.socialId(), data.profileImgUrl()),
                    data.deviceInfo()
            );
        }
    }
}
