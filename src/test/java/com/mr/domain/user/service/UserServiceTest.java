package com.mr.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mr.domain.user.dto.res.UserResponseDTO;
import com.mr.domain.user.entity.enums.UserRole;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.security.principal.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        CustomUserDetails userDetails = new CustomUserDetails(USER_ID, UserRole.ROLE_STUDENT);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("checkNicknameAvailable - 아무도(본인 포함) 안 쓰는 닉네임이면 isAvailable=true를 반환한다")
    void checkNicknameAvailable_notTaken_returnsAvailableTrue() {
        given(userRepository.existsByNicknameAndUserIdNot("김뮤즈", USER_ID)).willReturn(false);

        UserResponseDTO.NicknameCheckResponse response = userService.checkNicknameAvailable("김뮤즈");

        assertThat(response.nickname()).isEqualTo("김뮤즈");
        assertThat(response.isAvailable()).isTrue();
        assertThat(response.message()).isEqualTo("사용 가능한 닉네임입니다.");
    }

    @Test
    @DisplayName("checkNicknameAvailable - 다른 유저가 이미 쓰는 닉네임이면 예외 없이 isAvailable=false를 반환한다")
    void checkNicknameAvailable_takenByOtherUser_returnsAvailableFalse() {
        given(userRepository.existsByNicknameAndUserIdNot("김뮤즈", USER_ID)).willReturn(true);

        UserResponseDTO.NicknameCheckResponse response = userService.checkNicknameAvailable("김뮤즈");

        assertThat(response.isAvailable()).isFalse();
        assertThat(response.message()).isEqualTo("이미 사용 중인 닉네임입니다.");
    }

    @Test
    @DisplayName("checkNicknameAvailable - 본인이 이미 쓰고 있는 닉네임을 그대로 재확인하면 중복으로 처리하지 않는다")
    void checkNicknameAvailable_ownCurrentNickname_notTreatedAsDuplicate() {
        given(userRepository.existsByNicknameAndUserIdNot("김뮤즈", USER_ID)).willReturn(false);

        UserResponseDTO.NicknameCheckResponse response = userService.checkNicknameAvailable("김뮤즈");

        assertThat(response.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("checkNicknameAvailable - 앞뒤 공백은 trim한 뒤 조회하고 응답에도 trim된 값을 돌려준다")
    void checkNicknameAvailable_trimsWhitespaceBeforeLookup() {
        given(userRepository.existsByNicknameAndUserIdNot("김뮤즈", USER_ID)).willReturn(false);

        UserResponseDTO.NicknameCheckResponse response = userService.checkNicknameAvailable("  김뮤즈  ");

        assertThat(response.nickname()).isEqualTo("김뮤즈");
        verify(userRepository).existsByNicknameAndUserIdNot("김뮤즈", USER_ID);
    }

    @Test
    @DisplayName("checkNicknameAvailable - 닉네임이 null/공백이면 NICKNAME_REQUIRED 예외를 던지고 DB 조회는 하지 않는다")
    void checkNicknameAvailable_blank_throwsNicknameRequiredWithoutDbLookup() {
        assertThatThrownBy(() -> userService.checkNicknameAvailable("   "))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getCode())
                .isEqualTo(UserErrorStatus.NICKNAME_REQUIRED);

        verify(userRepository, never()).existsByNicknameAndUserIdNot(any(), any());
    }

    @Test
    @DisplayName("checkNicknameAvailable - 형식(한글/영문/숫자 2~10자)에 안 맞으면 NICKNAME_INVALID_FORMAT 예외를 던지고 DB 조회는 하지 않는다")
    void checkNicknameAvailable_invalidFormat_throwsInvalidFormatWithoutDbLookup() {
        assertThatThrownBy(() -> userService.checkNicknameAvailable("김@뮤즈"))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getCode())
                .isEqualTo(UserErrorStatus.NICKNAME_INVALID_FORMAT);

        verify(userRepository, never()).existsByNicknameAndUserIdNot(any(), any());
    }
}
