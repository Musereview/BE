package com.mr.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.mr.domain.auth.dto.OAuthUserInfo;
import com.mr.domain.auth.dto.res.AuthResponseDTO;
import com.mr.domain.auth.entity.SocialAuth;
import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.domain.auth.repository.SocialAuthRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.security.jwt.JwtTokenProvider;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class AuthTransactionServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private SocialAuthRepository socialAuthRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthTransactionService authTransactionService;

    @Test
    @DisplayName("completeTokenExchange - SocialAuth 저장 중 동시 요청으로 유니크 제약 위반이 나면, "
            + "이미 abort된 트랜잭션에서 복구 쿼리를 재시도하지 않고 DataIntegrityViolationException을 그대로 전파한다 "
            + "(호출 쪽인 AuthService가 새 트랜잭션에서 복구를 시도할 수 있도록)")
    void completeTokenExchange_socialAuthUniqueViolation_propagatesWithoutRetryingQuery() {
        User user = mock(User.class);
        given(user.getUserId()).willReturn(USER_ID);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        given(tokenProvider.createAccessToken(USER_ID)).willReturn("access-token");
        given(tokenProvider.createRefreshToken(USER_ID)).willReturn("refresh-token");
        given(tokenProvider.hashToken("refresh-token")).willReturn("hashed-refresh-token");
        given(tokenProvider.getRefreshTokenExpiryTime()).willReturn(Instant.now().plus(7, ChronoUnit.DAYS));

        // 최초 조회 시점엔 없다고 판단해 신규 생성 분기로 들어가지만,
        // 동시에 들어온 다른 요청이 먼저 저장을 마쳐서 실제 저장 시점엔 유니크 제약 위반이 남
        given(socialAuthRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, "social-1"))
                .willReturn(Optional.empty());
        given(socialAuthRepository.saveAndFlush(any())).willThrow(uniqueViolation());

        OAuthUserInfo userInfo = new OAuthUserInfo("social-1", "https://example.com/profile.png");

        assertThatThrownBy(() -> authTransactionService.completeTokenExchange(
                USER_ID, SocialType.KAKAO, userInfo.socialId(), userInfo.profileImgUrl(), "device"))
                .isInstanceOf(DataIntegrityViolationException.class);

        // 복구를 위해 같은 트랜잭션에서 findBySocialTypeAndSocialId를 다시 호출하지 않는지 확인
        // (딱 1번, 최초 조회 시점에만 호출됨 — 예전 버그였다면 catch 블록에서 한 번 더 호출돼 2번이 됨)
        verify(socialAuthRepository, times(1)).findBySocialTypeAndSocialId(SocialType.KAKAO, "social-1");
    }

    @Test
    @DisplayName("completeTokenExchange - 준비 후 다른 요청이 가입을 완료했으면 기존 사용자로 로그인")
    void completeTokenExchange_socialAuthCreatedAfterPrepare_usesExistingUser() {
        User existingUser = mock(User.class);
        SocialAuth existingSocialAuth = mock(SocialAuth.class);
        given(existingUser.getUserId()).willReturn(USER_ID);
        given(existingSocialAuth.getUser()).willReturn(existingUser);
        given(socialAuthRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, "social-1"))
                .willReturn(Optional.of(existingSocialAuth));
        given(tokenProvider.createAccessToken(USER_ID)).willReturn("access-token");
        given(tokenProvider.createRefreshToken(USER_ID)).willReturn("refresh-token");
        given(tokenProvider.hashToken("refresh-token")).willReturn("hashed-refresh-token");
        given(tokenProvider.getRefreshTokenExpiryTime()).willReturn(Instant.now().plus(7, ChronoUnit.DAYS));
        given(tokenProvider.getAccessTokenExpirationSeconds()).willReturn(1800L);

        AuthResponseDTO.LoginResponse response = authTransactionService.completeTokenExchange(
                null,
                SocialType.KAKAO,
                "social-1",
                "https://example.com/profile.png",
                "device"
        );

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.isNewUser()).isFalse();
        verify(userRepository, never()).save(any());
        verify(existingSocialAuth).updateRefreshToken(
                org.mockito.ArgumentMatchers.eq("hashed-refresh-token"),
                any(Instant.class),
                org.mockito.ArgumentMatchers.eq("device")
        );
    }

    private static DataIntegrityViolationException uniqueViolation() {
        SQLException sqlException = new SQLException("duplicate key value violates unique constraint", "23505");
        ConstraintViolationException constraintViolationException =
                new ConstraintViolationException("duplicate key", sqlException, "social_auth_type_id_key");
        return new DataIntegrityViolationException("duplicate key", constraintViolationException);
    }
}
