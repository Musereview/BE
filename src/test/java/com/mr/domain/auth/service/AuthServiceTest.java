package com.mr.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.mr.domain.auth.dto.OAuthUserInfo;
import com.mr.domain.auth.dto.res.AuthResponseDTO;
import com.mr.domain.auth.entity.SocialAuth;
import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.domain.auth.repository.SocialAuthRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.security.jwt.JwtTokenProvider;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private SocialAuthRepository socialAuthRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private OAuthClientService oAuthClientService;

    @InjectMocks
    private AuthService authService;

    private OAuthUserInfo userInfo;

    @BeforeEach
    void setUp() {
        userInfo = new OAuthUserInfo("12345", "https://example.com/profile.png");
    }

    @Test
    @DisplayName("socialLogin - 최초 로그인 시 isNewUser=true로 반환된다")
    void socialLogin_newUser_returnsIsNewUserTrue() {
        // given
        given(oAuthClientService.getUserInfo(SocialType.KAKAO, "access_token")).willReturn(userInfo);
        given(socialAuthRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, "12345"))
                .willReturn(Optional.empty());

        User newUser = User.createFromOAuth(userInfo.profileImgUrl());
        given(userRepository.save(any(User.class))).willReturn(newUser);
        given(tokenProvider.createAccessToken(any())).willReturn("new_access_token");
        given(tokenProvider.createRefreshToken(any())).willReturn("new_refresh_token");
        given(tokenProvider.hashToken(any())).willReturn("token_hash");
        given(tokenProvider.getRefreshTokenExpiryTime()).willReturn(LocalDateTime.now().plusDays(7));
        given(tokenProvider.getAccessTokenExpirationSeconds()).willReturn(3600L);

        SocialAuth savedSocialAuth = SocialAuth.create(
                newUser, SocialType.KAKAO, "12345", "new_refresh_token", "token_hash", LocalDateTime.now().plusDays(7), "deviceInfo"
        );
        given(socialAuthRepository.save(any(SocialAuth.class))).willReturn(savedSocialAuth);

        // when
        AuthResponseDTO.LoginResponse response = authService.socialLogin(SocialType.KAKAO, "access_token", "deviceInfo");

        // then
        assertThat(response.isNewUser()).isTrue();
        verify(userRepository).save(any(User.class));
        verify(socialAuthRepository).save(any(SocialAuth.class));
    }

    @Test
    @DisplayName("socialLogin - 기존 회원 로그인 시 isNewUser=false로 반환된다")
    void socialLogin_existingUser_returnsIsNewUserFalse() {
        // given
        given(oAuthClientService.getUserInfo(SocialType.KAKAO, "access_token")).willReturn(userInfo);

        User existingUser = User.createFromOAuth(userInfo.profileImgUrl());
        SocialAuth existingSocialAuth = SocialAuth.create(
                existingUser, SocialType.KAKAO, "12345", "old_token", "old_hash", LocalDateTime.now().plusDays(7), "deviceInfo"
        );
        given(socialAuthRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, "12345"))
                .willReturn(Optional.of(existingSocialAuth));

        given(tokenProvider.createAccessToken(any())).willReturn("new_access_token");
        given(tokenProvider.createRefreshToken(any())).willReturn("new_refresh_token");
        given(tokenProvider.hashToken(any())).willReturn("token_hash");
        given(tokenProvider.getRefreshTokenExpiryTime()).willReturn(LocalDateTime.now().plusDays(7));
        given(tokenProvider.getAccessTokenExpirationSeconds()).willReturn(3600L);

        // when
        AuthResponseDTO.LoginResponse response = authService.socialLogin(SocialType.KAKAO, "access_token", "deviceInfo");

        // then
        assertThat(response.isNewUser()).isFalse();
    }
}
