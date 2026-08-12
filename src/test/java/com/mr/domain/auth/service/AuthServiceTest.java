package com.mr.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;

import com.mr.domain.auth.dto.OAuthUserInfo;
import com.mr.domain.auth.dto.res.AuthResponseDTO;
import com.mr.domain.auth.entity.SocialAuth;
import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.domain.auth.repository.SocialAuthRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.security.jwt.JwtTokenProvider;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SocialAuthRepository socialAuthRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockBean
    private OAuthClientService oAuthClientService;

    @MockBean
    private OAuthTempCodeStore tempCodeStore;

    private OAuthUserInfo kakaoUserInfo;
    private final AtomicReference<String> storedTempCode = new AtomicReference<>();
    private final AtomicReference<OAuthTempCodeStore.TempExchangeData> storedTempData = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        kakaoUserInfo = new OAuthUserInfo("test_id_" + java.util.UUID.randomUUID(), "https://example.com/profile.png");
        storedTempCode.set(null);
        storedTempData.set(null);
        willAnswer(invocation -> {
            storedTempCode.set(invocation.getArgument(0));
            storedTempData.set(invocation.getArgument(1));
            return null;
        }).given(tempCodeStore).save(anyString(), any(OAuthTempCodeStore.TempExchangeData.class));
        given(tempCodeStore.consume(anyString())).willAnswer(invocation -> {
            if (!invocation.getArgument(0).equals(storedTempCode.get())) {
                return Optional.empty();
            }
            return Optional.ofNullable(storedTempData.getAndSet(null));
        });
    }

    @Test
    @DisplayName("socialLogin - 최초 소셜 로그인 시 DB에 User와 SocialAuth가 생성되고 isNewUser=true로 반환된다")
    void socialLogin_newUser_savesToDbAndReturnsIsNewUserTrue() {
        // given
        given(oAuthClientService.getUserInfo(SocialType.KAKAO, "access_token")).willReturn(kakaoUserInfo);

        // when
        AuthResponseDTO.LoginResponse response = authService.socialLogin(SocialType.KAKAO, "access_token", "deviceInfo");

        // then
        assertThat(response.isNewUser()).isTrue();
        assertThat(response.userId()).isNotNull();

        User savedUser = userRepository.findById(response.userId()).orElse(null);
        assertThat(savedUser).isNotNull();

        List<SocialAuth> socialAuths = socialAuthRepository.findAllByUser_UserId(response.userId());
        assertThat(socialAuths).hasSize(1);
        assertThat(socialAuths.get(0).getSocialId()).isEqualTo(kakaoUserInfo.socialId());
        assertThat(socialAuths.get(0).getSocialType()).isEqualTo(SocialType.KAKAO);
    }

    @Test
    @DisplayName("socialLogin - 이미 존재하는 계정으로 로그인 시 isNewUser=false로 반환된다")
    void socialLogin_existingUser_returnsIsNewUserFalse() {
        // given
        given(oAuthClientService.getUserInfo(SocialType.KAKAO, "access_token")).willReturn(kakaoUserInfo);
        authService.socialLogin(SocialType.KAKAO, "access_token", "deviceInfo"); // 최초 회원가입

        // when
        AuthResponseDTO.LoginResponse response = authService.socialLogin(SocialType.KAKAO, "access_token", "deviceInfo");

        // then
        assertThat(response.isNewUser()).isFalse();
    }

    @Test
    @DisplayName("reissueToken - 발급된 실제 Refresh Token으로 토큰 재발급 요청 시 새로운 토큰 세트가 발급되고 DB 해시가 업데이트된다")
    void reissueToken_realToken_reissuesTokensAndUpdateDbHash() throws InterruptedException {
        // given
        given(oAuthClientService.getUserInfo(SocialType.KAKAO, "access_token")).willReturn(kakaoUserInfo);
        AuthResponseDTO.LoginResponse loginResponse = authService.socialLogin(SocialType.KAKAO, "access_token", "deviceInfo");
        String originalRefreshToken = loginResponse.tokenInfo().refreshToken();
        String originalHash = tokenProvider.hashToken(originalRefreshToken);

        Thread.sleep(1005); // JWT issuedAt(초 단위) 타임스탬프 차이 보장

        // when
        AuthResponseDTO.TokenInfo reissuedTokenInfo = authService.reissueToken(originalRefreshToken);

        // then
        assertThat(reissuedTokenInfo.accessToken()).isNotBlank();
        assertThat(reissuedTokenInfo.refreshToken()).isNotBlank();
        assertThat(reissuedTokenInfo.refreshToken()).isNotEqualTo(originalRefreshToken);

        SocialAuth socialAuth = socialAuthRepository.findAllByUser_UserId(loginResponse.userId()).get(0);
        assertThat(socialAuth.getRefreshTokenHash()).isEqualTo(tokenProvider.hashToken(reissuedTokenInfo.refreshToken()));
        assertThat(socialAuth.getRefreshTokenHash()).isNotEqualTo(originalHash);
    }

    @Test
    @DisplayName("logout - 요청된 특정 Refresh Token 세션만 선택적으로 만료 처리한다")
    void logout_expiresSpecificDeviceSession() {
        // given
        given(oAuthClientService.getUserInfo(SocialType.KAKAO, "access_token")).willReturn(kakaoUserInfo);
        AuthResponseDTO.LoginResponse loginResponse = authService.socialLogin(SocialType.KAKAO, "access_token", "deviceInfo");
        Long userId = loginResponse.userId();
        String refreshToken = loginResponse.tokenInfo().refreshToken();

        // when
        authService.logout(userId, refreshToken);

        // then
        List<SocialAuth> socialAuths = socialAuthRepository.findAllByUser_UserId(userId);
        assertThat(socialAuths).hasSize(1);
        assertThat(socialAuths.get(0).getRefreshTokenHash()).isNull();
    }

    @Test
    @DisplayName("withdraw - 회원 탈퇴 시 DB에서 SocialAuth 및 User가 완전히 삭제된다")
    void withdraw_deletesSocialAuthAndUserFromDb() {
        // given
        given(oAuthClientService.getUserInfo(SocialType.KAKAO, "access_token")).willReturn(kakaoUserInfo);
        AuthResponseDTO.LoginResponse loginResponse = authService.socialLogin(SocialType.KAKAO, "access_token", "deviceInfo");
        Long userId = loginResponse.userId();

        // when
        authService.withdraw(userId);

        // then
        assertThat(userRepository.findById(userId)).isEmpty();
        assertThat(socialAuthRepository.findAllByUser_UserId(userId)).isEmpty();
    }

    @Test
    @DisplayName("linkSocialAccount - 기존 사용자 계정에 다른 소셜 계정(구글)을 추가 연동할 수 있다")
    void linkSocialAccount_existingUser_addsSecondSocialAuth() {
        // given
        OAuthUserInfo testKakaoInfo = new OAuthUserInfo("kakao_link_" + java.util.UUID.randomUUID(), "https://example.com/kakao.png");
        given(oAuthClientService.getUserInfo(SocialType.KAKAO, "kakao_token")).willReturn(testKakaoInfo);
        AuthResponseDTO.LoginResponse loginResponse = authService.socialLogin(SocialType.KAKAO, "kakao_token", "deviceInfo");
        Long userId = loginResponse.userId();

        OAuthUserInfo googleUserInfo = new OAuthUserInfo("google_link_" + java.util.UUID.randomUUID(), "https://example.com/google.png");
        given(oAuthClientService.getUserInfo(SocialType.GOOGLE, "google_token")).willReturn(googleUserInfo);

        // when
        AuthResponseDTO.TokenInfo tokenInfo = authService.linkSocialAccount(userId, SocialType.GOOGLE, "google_token", "deviceInfo");

        // then
        assertThat(tokenInfo.accessToken()).isNotBlank();
        List<SocialAuth> userSocialAuths = socialAuthRepository.findAllByUser_UserId(userId);
        assertThat(userSocialAuths).hasSize(2);
    }

    @Test
    @DisplayName("linkSocialAccount - 동일한 socialType의 다른 계정을 이미 연동한 경우 ALREADY_LINKED_SOCIAL_ACCOUNT 예외가 발생한다")
    void linkSocialAccount_sameSocialTypeDifferentAccount_throwsException() {
        // given
        OAuthUserInfo firstKakaoInfo = new OAuthUserInfo("kakao_1_" + java.util.UUID.randomUUID(), "https://example.com/kakao1.png");
        given(oAuthClientService.getUserInfo(SocialType.KAKAO, "token_1")).willReturn(firstKakaoInfo);
        AuthResponseDTO.LoginResponse loginResponse = authService.socialLogin(SocialType.KAKAO, "token_1", "deviceInfo");
        Long userId = loginResponse.userId();

        OAuthUserInfo secondKakaoInfo = new OAuthUserInfo("kakao_2_" + java.util.UUID.randomUUID(), "https://example.com/kakao2.png");
        given(oAuthClientService.getUserInfo(SocialType.KAKAO, "token_2")).willReturn(secondKakaoInfo);

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> authService.linkSocialAccount(userId, SocialType.KAKAO, "token_2", "deviceInfo"))
                .isInstanceOf(com.mr.global.apipayload.exception.GeneralException.class)
                .satisfies(e -> assertThat(((com.mr.global.apipayload.exception.GeneralException) e).getCode()).isEqualTo(com.mr.domain.auth.exception.AuthErrorStatus.ALREADY_LINKED_SOCIAL_ACCOUNT));
    }

    @Test
    @DisplayName("generateTempCodeByCode / exchangeTempCode - 콜백 단계에서는 최소 정보만 보관하고, /token/exchange 시점에 원자적으로 Refresh Token을 DB에 저장한다")
    void tempExchangeCode_atomicTokenExchangeSuccess() {
        // given
        given(oAuthClientService.getUserInfoByCode(SocialType.KAKAO, "sample_code", null)).willReturn(kakaoUserInfo);

        // when - 콜백 시점: 임시 코드 생성
        String tempCode = authService.generateTempCodeByCode(SocialType.KAKAO, "sample_code", null, "deviceInfo");
        assertThat(tempCode).isNotBlank();

        // when - /token/exchange 시점: 토큰 교환 & DB Hash 갱신
        AuthResponseDTO.LoginResponse response = authService.exchangeTempCode(tempCode);

        // then
        assertThat(response.tokenInfo().accessToken()).isNotBlank();
        assertThat(response.tokenInfo().refreshToken()).isNotBlank();

        List<SocialAuth> socialAuths = socialAuthRepository.findAllByUser_UserId(response.userId());
        assertThat(socialAuths).hasSize(1);
        assertThat(socialAuths.get(0).getRefreshTokenHash()).isEqualTo(tokenProvider.hashToken(response.tokenInfo().refreshToken()));

        // then - 2차 재사용 시 예외 발생
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> authService.exchangeTempCode(tempCode))
                .isInstanceOf(com.mr.global.apipayload.exception.GeneralException.class);
    }
}
