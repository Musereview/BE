package com.mr.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.user.dto.req.UserProfileRequestDTO;
import com.mr.domain.user.dto.res.UserProfileResponseDTO;
import com.mr.domain.user.entity.enums.TheoryLevel;
import com.mr.domain.user.exception.StudentErrorStatus;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.service.UserProfileService;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.apipayload.handler.GlobalExceptionHandler;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserProfileService userProfileService;

    private MockMvc setUp() {
        objectMapper = Jackson2ObjectMapperBuilder.json()
                .findModulesViaServiceLoader(true)
                .build();

        return MockMvcBuilders.standaloneSetup(new UserProfileController(userProfileService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("GET /api/users/me/profile - 조회 성공")
    void getMyProfile_success() throws Exception {
        mockMvc = setUp();

        UserProfileResponseDTO.ProfileResponse response = UserProfileResponseDTO.ProfileResponse.builder()
                .nickname("김뮤즈")
                .profileImgUrl("https://cdn.example.com/profile/default-profile.png")
                .instrumentType("PIANO")
                .skillLevel(TheoryLevel.INTERMEDIATE)
                .subscriptionTier("PRO")
                .statistics(UserProfileResponseDTO.StatisticsResponse.builder()
                        .practiceSessionCount(24L)
                        .totalPracticeMinutes(350L)
                        .completedLearningCount(0L)
                        .build())
                .build();
        given(userProfileService.getMyProfile()).willReturn(response);

        mockMvc.perform(get("/api/users/me/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.nickname").value("김뮤즈"))
                .andExpect(jsonPath("$.data.instrumentType").value("PIANO"))
                .andExpect(jsonPath("$.data.subscriptionTier").value("PRO"))
                .andExpect(jsonPath("$.data.statistics.practiceSessionCount").value(24));
    }

    @Test
    @DisplayName("GET /api/users/me/profile - user 없으면 404")
    void getMyProfile_userNotFound() throws Exception {
        mockMvc = setUp();

        given(userProfileService.getMyProfile())
                .willThrow(new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        mockMvc.perform(get("/api/users/me/profile"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_404_01"));
    }

    @Test
    @DisplayName("GET /api/users/me/profile - 온보딩 안 된 유저면 404")
    void getMyProfile_studentNotFound() throws Exception {
        mockMvc = setUp();

        given(userProfileService.getMyProfile())
                .willThrow(new GeneralException(StudentErrorStatus.STUDENT_NOT_FOUND));

        mockMvc.perform(get("/api/users/me/profile"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_404_01"));
    }

    @Test
    @DisplayName("POST /api/users/me/profile - 최초 등록 성공")
    void registerProfile_success() throws Exception {
        mockMvc = setUp();

        UserProfileRequestDTO.OnboardingRequest request = new UserProfileRequestDTO.OnboardingRequest(
                "김뮤즈", TheoryLevel.INTERMEDIATE
        );
        UserProfileResponseDTO.OnboardingResponse response = UserProfileResponseDTO.OnboardingResponse.builder()
                .userId(1L)
                .nickname("김뮤즈")
                .skillLevel(TheoryLevel.INTERMEDIATE)
                .instrumentType("PIANO")
                .subscriptionTier("PRO")
                .profileImgUrl("https://cdn.example.com/profile/default-profile.png")
                .build();
        given(userProfileService.registerProfile(any())).willReturn(response);

        mockMvc.perform(post("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.nickname").value("김뮤즈"))
                .andExpect(jsonPath("$.data.instrumentType").value("PIANO"));
    }

    @Test
    @DisplayName("POST /api/users/me/profile - 이미 온보딩 완료했으면 409")
    void registerProfile_alreadyCompleted() throws Exception {
        mockMvc = setUp();

        UserProfileRequestDTO.OnboardingRequest request = new UserProfileRequestDTO.OnboardingRequest(
                "김뮤즈", TheoryLevel.INTERMEDIATE
        );
        willThrow(new GeneralException(UserErrorStatus.ONBOARDING_ALREADY_COMPLETED))
                .given(userProfileService).registerProfile(any());

        mockMvc.perform(post("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_409_02"));
    }

    @Test
    @DisplayName("POST /api/users/me/profile - 닉네임 중복이면 409")
    void registerProfile_nicknameDuplicated() throws Exception {
        mockMvc = setUp();

        UserProfileRequestDTO.OnboardingRequest request = new UserProfileRequestDTO.OnboardingRequest(
                "김뮤즈", TheoryLevel.INTERMEDIATE
        );
        willThrow(new GeneralException(UserErrorStatus.NICKNAME_DUPLICATED))
                .given(userProfileService).registerProfile(any());

        mockMvc.perform(post("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_409_01"));
    }

    @Test
    @DisplayName("PATCH /api/users/me/profile - 수정 성공")
    void updateProfile_success() throws Exception {
        mockMvc = setUp();

        UserProfileRequestDTO.UpdateRequest request = new UserProfileRequestDTO.UpdateRequest(
                "새로운뮤즈", TheoryLevel.ADVANCED
        );
        UserProfileResponseDTO.UpdateResponse response = UserProfileResponseDTO.UpdateResponse.builder()
                .userId(1L)
                .nickname("새로운뮤즈")
                .skillLevel(TheoryLevel.ADVANCED)
                .updatedAt(LocalDateTime.of(2026, 7, 24, 1, 30))
                .build();
        given(userProfileService.updateProfile(any())).willReturn(response);

        mockMvc.perform(patch("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.nickname").value("새로운뮤즈"))
                .andExpect(jsonPath("$.data.skillLevel").value("ADVANCED"));
    }

    @Test
    @DisplayName("PATCH /api/users/me/profile - 온보딩 안 된 유저면 404")
    void updateProfile_studentNotFound() throws Exception {
        mockMvc = setUp();

        UserProfileRequestDTO.UpdateRequest request = new UserProfileRequestDTO.UpdateRequest(
                "새로운뮤즈", TheoryLevel.ADVANCED
        );
        willThrow(new GeneralException(StudentErrorStatus.STUDENT_NOT_FOUND))
                .given(userProfileService).updateProfile(any());

        mockMvc.perform(patch("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_404_01"));
    }

    @Test
    @DisplayName("PATCH /api/users/me/profile - 닉네임 중복이면 409")
    void updateProfile_nicknameDuplicated() throws Exception {
        mockMvc = setUp();

        UserProfileRequestDTO.UpdateRequest request = new UserProfileRequestDTO.UpdateRequest(
                "새로운뮤즈", TheoryLevel.ADVANCED
        );
        willThrow(new GeneralException(UserErrorStatus.NICKNAME_DUPLICATED))
                .given(userProfileService).updateProfile(any());

        mockMvc.perform(patch("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_409_01"));
    }
}
