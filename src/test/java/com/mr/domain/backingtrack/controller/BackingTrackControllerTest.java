package com.mr.domain.backingtrack.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.backingtrack.dto.req.BackingTrackSaveRequestDTO;
import com.mr.domain.backingtrack.dto.res.BackingTrackCreateResponseDTO;
import com.mr.domain.backingtrack.dto.res.BackingTrackUpdateResponseDTO;
import com.mr.domain.backingtrack.entity.enums.AccessLevel;
import com.mr.domain.backingtrack.entity.enums.Level;
import com.mr.domain.backingtrack.entity.enums.ScaleType;
import com.mr.domain.backingtrack.exception.BackingTrackErrorStatus;
import com.mr.domain.backingtrack.service.BackingTrackService;
import com.mr.domain.user.entity.enums.UserRole;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.apipayload.handler.GlobalExceptionHandler;
import com.mr.global.security.principal.CustomUserDetails;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class BackingTrackControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private BackingTrackService backingTrackService;

    private BackingTrackSaveRequestDTO.SaveDTO validRequest;

    @BeforeEach
    void setUp() {
        objectMapper = Jackson2ObjectMapperBuilder.json()
                .findModulesViaServiceLoader(true)
                .build();

        // 💡 Security Principal을 읽어오도록 ArgumentResolver 등록
        mockMvc = MockMvcBuilders.standaloneSetup(new BackingTrackController(backingTrackService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.ROLE_STUDENT);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities())
        );

        validRequest = new BackingTrackSaveRequestDTO.SaveDTO(
                "테스트 백킹트랙", "Jazz", "C", ScaleType.MAJOR, "4/4", 120, 180, "http://audio.url",
                AccessLevel.PUBLIC, Level.BASIC,
                List.of(new BackingTrackSaveRequestDTO.ChordProgressionDTO(1, 1, "CM7"))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/backing-tracks - 백킹트랙 생성 성공")
    void createBackingTrack_success() throws Exception {
        BackingTrackCreateResponseDTO.CreateResultDTO response = new BackingTrackCreateResponseDTO.CreateResultDTO(
                1L, "테스트 백킹트랙", LocalDateTime.of(2026, 7, 25, 10, 0)
        );
        given(backingTrackService.createBackingTrack(anyLong(), any())).willReturn(response);

        mockMvc.perform(post("/api/backing-tracks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.backingTrackId").value(1L))
                .andExpect(jsonPath("$.data.title").value("테스트 백킹트랙"));
    }

    @Test
    @DisplayName("PUT /api/backing-tracks/{id} - 백킹트랙 수정 성공")
    void updateBackingTrack_success() throws Exception {
        BackingTrackUpdateResponseDTO.UpdateResultDTO response = new BackingTrackUpdateResponseDTO.UpdateResultDTO(
                1L, "테스트 백킹트랙", LocalDateTime.of(2026, 7, 25, 12, 0)
        );
        given(backingTrackService.updateBackingTrack(anyLong(), anyLong(), any())).willReturn(response);

        mockMvc.perform(put("/api/backing-tracks/{backingTrackId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.backingTrackId").value(1L));
    }

    @Test
    @DisplayName("PUT /api/backing-tracks/{id} - 권한이 없는 사용자면 403")
    void updateBackingTrack_forbidden() throws Exception {
        willThrow(new GeneralException(BackingTrackErrorStatus.FORBIDDEN_UPDATE))
                .given(backingTrackService).updateBackingTrack(anyLong(), anyLong(), any());

        mockMvc.perform(put("/api/backing-tracks/{backingTrackId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(BackingTrackErrorStatus.FORBIDDEN_UPDATE.getCode()));
    }
}