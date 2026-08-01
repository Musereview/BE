package com.mr.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mr.domain.user.dto.res.UserResponseDTO;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("checkNicknameAvailable - 아무도 안 쓰는 닉네임이면 available=true를 반환한다")
    void checkNicknameAvailable_notTaken_returnsAvailableTrue() {
        given(userRepository.existsByNickname("김뮤즈")).willReturn(false);

        UserResponseDTO.NicknameCheckResponse response = userService.checkNicknameAvailable("김뮤즈");

        assertThat(response.nickname()).isEqualTo("김뮤즈");
        assertThat(response.available()).isTrue();
    }

    @Test
    @DisplayName("checkNicknameAvailable - 이미 사용 중인 닉네임이면 available=false를 반환한다")
    void checkNicknameAvailable_alreadyTaken_returnsAvailableFalse() {
        given(userRepository.existsByNickname("김뮤즈")).willReturn(true);

        UserResponseDTO.NicknameCheckResponse response = userService.checkNicknameAvailable("김뮤즈");

        assertThat(response.available()).isFalse();
    }

    @Test
    @DisplayName("checkNicknameAvailable - 앞뒤 공백은 trim한 뒤 조회하고 응답에도 trim된 값을 돌려준다")
    void checkNicknameAvailable_trimsWhitespaceBeforeLookup() {
        given(userRepository.existsByNickname("김뮤즈")).willReturn(false);

        UserResponseDTO.NicknameCheckResponse response = userService.checkNicknameAvailable("  김뮤즈  ");

        assertThat(response.nickname()).isEqualTo("김뮤즈");
        verify(userRepository).existsByNickname("김뮤즈");
    }

    @Test
    @DisplayName("checkNicknameAvailable - 닉네임이 null/공백이면 NICKNAME_REQUIRED 예외를 던지고 DB 조회는 하지 않는다")
    void checkNicknameAvailable_blank_throwsNicknameRequiredWithoutDbLookup() {
        assertThatThrownBy(() -> userService.checkNicknameAvailable("   "))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getCode())
                .isEqualTo(UserErrorStatus.NICKNAME_REQUIRED);

        verify(userRepository, never()).existsByNickname(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("checkNicknameAvailable - 형식(한글/영문/숫자 2~10자)에 안 맞으면 NICKNAME_INVALID_FORMAT 예외를 던지고 DB 조회는 하지 않는다")
    void checkNicknameAvailable_invalidFormat_throwsInvalidFormatWithoutDbLookup() {
        assertThatThrownBy(() -> userService.checkNicknameAvailable("김@뮤즈"))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getCode())
                .isEqualTo(UserErrorStatus.NICKNAME_INVALID_FORMAT);

        verify(userRepository, never()).existsByNickname(org.mockito.ArgumentMatchers.any());
    }
}
