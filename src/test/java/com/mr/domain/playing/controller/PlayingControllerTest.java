package com.mr.domain.playing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.playing.dto.req.MidiEventSaveRequest;
import com.mr.domain.playing.dto.res.MidiEventSaveResponse;
import com.mr.domain.playing.entity.enums.MidiType;
import com.mr.domain.playing.service.PlayingService;
import com.mr.domain.user.entity.enums.UserRole;
import com.mr.global.apipayload.handler.GlobalExceptionHandler;
import com.mr.global.security.principal.CustomUserDetails;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PlayingControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private PlayingService playingService;

    @BeforeEach
    void setUp() {
        objectMapper = Jackson2ObjectMapperBuilder.json()
                .findModulesViaServiceLoader(true)
                .build();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new PlayingController(playingService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(
                        new MappingJackson2HttpMessageConverter(objectMapper)
                )
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver()
                )
                .build();

        CustomUserDetails userDetails =
                new CustomUserDetails(
                        1L,
                        UserRole.ROLE_STUDENT
                );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        "",
                        userDetails.getAuthorities()
                )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/playings/{playingId}/midi-events - MIDI 이벤트 저장 성공")
    void saveMidiEvents_success() throws Exception {
        // given
        Long userId = 1L;
        Long playingId = 10L;

        MidiEventSaveRequest request = createRequest();

        MidiEventSaveResponse response =
                new MidiEventSaveResponse(
                        playingId,
                        2
                );

        given(
                playingService.saveMidiEvents(
                        anyLong(),
                        anyLong(),
                        any(MidiEventSaveRequest.class)
                )
        ).willReturn(response);

        // when & then
        mockMvc.perform(
                        post(
                                "/api/playings/{playingId}/midi-events",
                                playingId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(
                        jsonPath("$.data.playingId")
                                .value(playingId)
                )
                .andExpect(
                        jsonPath("$.data.savedCount")
                                .value(2)
                );

        then(playingService)
                .should()
                .saveMidiEvents(
                        userId,
                        playingId,
                        request
                );
    }

    private MidiEventSaveRequest createRequest() {
        return new MidiEventSaveRequest(
                List.of(
                        new MidiEventSaveRequest.MidiEventRequest(
                                0,
                                MidiType.NOTE_ON,
                                60,
                                100,
                                0L
                        ),
                        new MidiEventSaveRequest.MidiEventRequest(
                                1,
                                MidiType.NOTE_OFF,
                                60,
                                0,
                                500L
                        )
                )
        );
    }
}