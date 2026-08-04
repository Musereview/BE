package com.mr.domain.mentor.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mr.domain.mentor.dto.res.MentorMessageHistoryResponseDTO;
import com.mr.domain.mentor.entity.enums.MessageRole;
import com.mr.domain.mentor.exception.MentorErrorStatus;
import com.mr.domain.mentor.service.MentorService;
import com.mr.domain.mentor.service.MentorQuestionService;
import com.mr.domain.mentor.service.MentorStreamingService;
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
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class MentorControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MentorService mentorService;

    @Mock
    private MentorQuestionService mentorQuestionService;

    @Mock
    private MentorStreamingService mentorStreamingService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
                .findModulesViaServiceLoader(true)
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new MentorController(mentorService, mentorQuestionService, mentorStreamingService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.ROLE_STUDENT);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/analyses/{id}/mentor/messages - 대화 내역 조회 성공")
    void getMessageHistory_success() throws Exception {
        ObjectNode references = new ObjectMapper().createObjectNode();
        references.putArray("sourceFields").add("raw_result_json.scores.axes.vocabulary");
        MentorMessageHistoryResponseDTO response = new MentorMessageHistoryResponseDTO(
                10L,
                List.of(new MentorMessageHistoryResponseDTO.Message(
                        1L,
                        MessageRole.ASSISTANT,
                        references,
                        "텐션음을 자주 사용했어요.",
                        LocalDateTime.of(2026, 7, 1, 14, 35)
                ))
        );
        given(mentorService.getMessageHistory(anyLong(), anyLong())).willReturn(response);

        mockMvc.perform(get("/api/analyses/{analysisId}/mentor/messages", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.analysisId").value(10L))
                .andExpect(jsonPath("$.data.messages[0].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data.messages[0].referencesJson.sourceFields[0]")
                        .value("raw_result_json.scores.axes.vocabulary"));
    }

    @Test
    @DisplayName("GET /api/analyses/{id}/mentor/messages - 대화가 없으면 빈 배열 반환")
    void getMessageHistory_empty() throws Exception {
        given(mentorService.getMessageHistory(anyLong(), anyLong()))
                .willReturn(new MentorMessageHistoryResponseDTO(10L, List.of()));

        mockMvc.perform(get("/api/analyses/{analysisId}/mentor/messages", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages").isArray())
                .andExpect(jsonPath("$.data.messages").isEmpty());
    }

    @Test
    @DisplayName("POST /api/analyses/{id}/mentor/messages - SSE 질문 전송 시작")
    void sendQuestion_startsSseStream() throws Exception {
        MentorQuestionService.PreparedQuestion prepared = new MentorQuestionService.PreparedQuestion(
                3L,
                "generation-token",
                "prompt",
                new com.mr.domain.mentor.dto.res.MentorStreamEventDTO.Start(10L, 3L, null)
        );
        given(mentorQuestionService.prepare(1L, 10L, "텐션음을 더 써도 되나요?"))
                .willReturn(prepared);
        given(mentorStreamingService.stream(prepared)).willReturn(new SseEmitter());

        mockMvc.perform(post("/api/analyses/{analysisId}/mentor/messages", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("""
                                {"content":"텐션음을 더 써도 되나요?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }

    @Test
    @DisplayName("POST /api/analyses/{id}/mentor/messages - 소유권 검증 실패 시 JSON 403을 반환한다")
    void sendQuestion_accessDenied_returnsJsonForbidden() throws Exception {
        given(mentorQuestionService.prepare(1L, 10L, "질문"))
                .willThrow(new GeneralException(MentorErrorStatus.MENTOR_ACCESS_DENIED));

        mockMvc.perform(post("/api/analyses/{analysisId}/mentor/messages", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("""
                                {"content":"질문"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("MENTOR_403_01"));

        verifyNoInteractions(mentorStreamingService);
    }
}
