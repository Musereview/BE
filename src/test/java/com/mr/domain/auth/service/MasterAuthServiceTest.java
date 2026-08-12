package com.mr.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.mr.domain.auth.dto.res.MasterAuthResponse;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MasterAuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock JwtTokenProvider jwtTokenProvider;
    @InjectMocks MasterAuthService masterAuthService;

    @Test
    @DisplayName("존재하는 사용자 ID로 기존 체계의 Access Token을 발급한다")
    void issueAccessToken_existingUser_returnsAccessToken() {
        given(userRepository.existsById(1L)).willReturn(true);
        given(jwtTokenProvider.createAccessToken(1L)).willReturn("access-token");
        given(jwtTokenProvider.getAccessTokenExpirationSeconds()).willReturn(1800L);

        MasterAuthResponse response = masterAuthService.issueAccessToken(1L);

        assertThat(response).isEqualTo(new MasterAuthResponse(1L, "access-token", 1800L));
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID면 토큰을 발급하지 않는다")
    void issueAccessToken_missingUser_throwsNotFound() {
        given(userRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> masterAuthService.issueAccessToken(1L))
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> assertThat(((GeneralException) exception).getCode())
                        .isEqualTo(UserErrorStatus.USER_NOT_FOUND));
        verifyNoInteractions(jwtTokenProvider);
    }
}
