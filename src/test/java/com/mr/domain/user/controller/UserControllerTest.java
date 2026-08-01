package com.mr.domain.user.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mr.domain.user.dto.res.UserResponseDTO;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.service.UserService;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.apipayload.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    private MockMvc setUp() {
        return MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/users/verify-nickname - nickname 파라미터를 정상적으로 넘기면 200")
    void verifyNickname_success() throws Exception {
        mockMvc = setUp();

        UserResponseDTO.NicknameCheckResponse response = UserResponseDTO.NicknameCheckResponse.builder()
                .nickname("김뮤즈")
                .isAvailable(true)
                .message("사용 가능한 닉네임입니다.")
                .build();
        given(userService.checkNicknameAvailable("김뮤즈")).willReturn(response);

        mockMvc.perform(get("/api/users/verify-nickname").param("nickname", "김뮤즈"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.nickname").value("김뮤즈"))
                .andExpect(jsonPath("$.data.isAvailable").value(true))
                .andExpect(jsonPath("$.data.message").value("사용 가능한 닉네임입니다."));
    }

    @Test
    @DisplayName("GET /api/users/verify-nickname - 다른 유저가 이미 쓰는 닉네임이면 409로 응답한다")
    void verifyNickname_duplicated_returns409() throws Exception {
        mockMvc = setUp();

        willThrow(new GeneralException(UserErrorStatus.NICKNAME_DUPLICATED))
                .given(userService).checkNicknameAvailable("김뮤즈");

        mockMvc.perform(get("/api/users/verify-nickname").param("nickname", "김뮤즈"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_409_01"));
    }

    @Test
    @DisplayName("GET /api/users/verify-nickname - nickname 파라미터 자체가 누락되면 400 NICKNAME_REQUIRED로 응답한다 (500 아님)")
    void verifyNickname_missingParam_returns400NicknameRequired() throws Exception {
        mockMvc = setUp();

        willThrow(new GeneralException(UserErrorStatus.NICKNAME_REQUIRED))
                .given(userService).checkNicknameAvailable(eq(null));

        mockMvc.perform(get("/api/users/verify-nickname"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_400_01"));
    }

    @Test
    @DisplayName("GET /api/users/verify-nickname - nickname이 빈 문자열이면 400 NICKNAME_REQUIRED로 응답한다")
    void verifyNickname_blankParam_returns400NicknameRequired() throws Exception {
        mockMvc = setUp();

        willThrow(new GeneralException(UserErrorStatus.NICKNAME_REQUIRED))
                .given(userService).checkNicknameAvailable("   ");

        mockMvc.perform(get("/api/users/verify-nickname").param("nickname", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_400_01"));
    }

    @Test
    @DisplayName("GET /api/users/verify-nickname - 형식 위반이면 400 NICKNAME_INVALID_FORMAT으로 응답한다")
    void verifyNickname_invalidFormat_returns400InvalidFormat() throws Exception {
        mockMvc = setUp();

        willThrow(new GeneralException(UserErrorStatus.NICKNAME_INVALID_FORMAT))
                .given(userService).checkNicknameAvailable("김@뮤즈");

        mockMvc.perform(get("/api/users/verify-nickname").param("nickname", "김@뮤즈"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_400_02"));
    }
}
