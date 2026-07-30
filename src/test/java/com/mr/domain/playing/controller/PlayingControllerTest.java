package com.mr.domain.playing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.playing.dto.req.MidiEventSaveRequest;
import com.mr.domain.playing.dto.res.MidiEventSaveResponse;
import com.mr.domain.playing.entity.enums.MidiType;
import com.mr.domain.playing.service.PlayingService;
import com.mr.domain.user.entity.enums.UserRole;
import com.mr.global.apipayload.handler.GlobalExceptionHandler;
import com.mr.global.security.principal.CustomUserDetails;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
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

import static com.mr.domain.playing.constant.MidiEventConstants.MAX_MIDI_EVENT_COUNT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PlayingControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long PLAYING_ID = 10L;


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

    @Nested
    @DisplayName("MIDI 이벤트 저장 성공")
    class SaveMidiEventsSuccess {
        @Test
        @DisplayName("POST /api/playings/{playingId}/midi-events - MIDI 이벤트 저장 성공")
        void saveMidiEvents_success() throws Exception {
            // given

            MidiEventSaveRequest request = createRequest();

            MidiEventSaveResponse response =
                    new MidiEventSaveResponse(
                            PLAYING_ID,
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
                                    PLAYING_ID
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
                                    .value(PLAYING_ID)
                    )
                    .andExpect(
                            jsonPath("$.data.savedCount")
                                    .value(2)
                    );

            then(playingService)
                    .should()
                    .saveMidiEvents(
                            USER_ID,
                            PLAYING_ID,
                            request
                    );
        }
    }

    @Nested
    @DisplayName("MIDI 이벤트 저장 요청 검증 실패")
    class SaveMidiEventsValidationFailure {

        @Test
        @DisplayName("events 필드가 누락되면 요청에 실패한다")
        void saveMidiEvents_eventsMissing() throws Exception {
            // given
            String requestBody = """
                    {
                    }
                    """;

            // when & then
            mockMvc.perform(
                            post(
                                    "/api/playings/{playingId}/midi-events",
                                    PLAYING_ID
                            )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));

            verifyServiceNotCalled();
        }

        @Test
        @DisplayName("events가 빈 배열이면 요청에 실패한다")
        void saveMidiEvents_eventsEmpty() throws Exception {
            // given
            String requestBody = """
                    {
                      "events": []
                    }
                    """;

            // when & then
            mockMvc.perform(
                            post(
                                    "/api/playings/{playingId}/midi-events",
                                    PLAYING_ID
                            )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));

            verifyServiceNotCalled();
        }

        @Test
        @DisplayName("MIDI 이벤트 sequence가 음수이면 요청에 실패한다")
        void saveMidiEvents_negativeSequence() throws Exception {
            String requestBody = """
            {
              "events": [
                {
                  "sequence": -1,
                  "type": "NOTE_ON",
                  "pitch": 60,
                  "velocity": 100,
                  "timestampMs": 0
                }
              ]
            }
            """;

            mockMvc.perform(
                            post(
                                    "/api/playings/{playingId}/midi-events",
                                    PLAYING_ID
                            )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(
                            jsonPath("$.isSuccess")
                                    .value(false)
                    );

            verifyServiceNotCalled();
        }

        @Test
        @DisplayName("정의되지 않은 MidiType이면 공통 예외 응답을 반환한다")
        void saveMidiEvents_invalidMidiType() throws Exception {
            // given
            String requestBody = """
                    {
                      "events": [
                        {
                          "sequence": 0,
                          "type": "INVALID_TYPE",
                          "pitch": 60,
                          "velocity": 100,
                          "timestampMs": 0
                        }
                      ]
                    }
                    """;

            // when & then
            mockMvc.perform(
                            post(
                                    "/api/playings/{playingId}/midi-events",
                                    PLAYING_ID
                            )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").exists());

            verifyServiceNotCalled();
        }

        @Test
        @DisplayName("MIDI 이벤트가 최대 개수를 초과하면 요청에 실패한다")
        void saveMidiEvents_eventCountExceeded() throws Exception {
            // given
            List<MidiEventSaveRequest.MidiEventRequest> events =
                    new ArrayList<>(MAX_MIDI_EVENT_COUNT + 1);

            for (int sequence = 0;
                 sequence <= MAX_MIDI_EVENT_COUNT;
                 sequence++) {

                events.add(
                        new MidiEventSaveRequest.MidiEventRequest(
                                sequence,
                                MidiType.NOTE_ON,
                                60,
                                100,
                                0L
                        )
                );
            }

            MidiEventSaveRequest request =
                    new MidiEventSaveRequest(events);

            // when & then
            mockMvc.perform(
                            post(
                                    "/api/playings/{playingId}/midi-events",
                                    PLAYING_ID
                            )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            objectMapper.writeValueAsString(request)
                                    )
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));

            verifyServiceNotCalled();
        }

        private void verifyServiceNotCalled() {
            then(playingService)
                    .should(never())
                    .saveMidiEvents(
                            anyLong(),
                            anyLong(),
                            any(MidiEventSaveRequest.class)
                    );
        }
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