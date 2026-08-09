package com.mr.domain.mentor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.analysis.exception.AnalysisErrorStatus;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.domain.mentor.dto.res.MentorMessageHistoryResponseDTO;
import com.mr.domain.mentor.entity.MentorMessage;
import com.mr.domain.mentor.entity.enums.MessageRole;
import com.mr.domain.mentor.exception.MentorErrorStatus;
import com.mr.domain.mentor.repository.MentorMessageRepository;
import com.mr.global.apipayload.exception.GeneralException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MentorServiceTest {

    @Mock
    private AnalysisRepository analysisRepository;

    @Mock
    private MentorMessageRepository mentorMessageRepository;

    private MentorService mentorService;

    @BeforeEach
    void setUp() {
        mentorService = new MentorService(
                analysisRepository,
                mentorMessageRepository,
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("대화 세션이 없으면 빈 메시지 목록 반환")
    void getMessageHistory_noSession_returnsEmptyMessages() {
        given(analysisRepository.findUserIdById(10L)).willReturn(Optional.of(1L));
        given(mentorMessageRepository.findByMentorChatSessionAnalysisIdOrderByCreatedAtAscIdAsc(10L))
                .willReturn(List.of());

        MentorMessageHistoryResponseDTO response = mentorService.getMessageHistory(1L, 10L);

        assertThat(response.analysisId()).isEqualTo(10L);
        assertThat(response.messages()).isEmpty();
    }

    @Test
    @DisplayName("대화 메시지와 판단 근거 JSON 반환")
    void getMessageHistory_success() {
        MentorMessage message = mock(MentorMessage.class);
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 1, 14, 35);

        given(message.getId()).willReturn(30L);
        given(message.getRole()).willReturn(MessageRole.ASSISTANT);
        given(message.getReferencesJson()).willReturn("""
                {"sourceFields":["raw_result_json.scores.axes.vocabulary"]}
                """);
        given(message.getContent()).willReturn("텐션음을 자주 사용했어요.");
        given(message.getCreatedAt()).willReturn(createdAt);
        given(analysisRepository.findUserIdById(10L)).willReturn(Optional.of(1L));
        given(mentorMessageRepository.findByMentorChatSessionAnalysisIdOrderByCreatedAtAscIdAsc(10L))
                .willReturn(List.of(message));

        MentorMessageHistoryResponseDTO response = mentorService.getMessageHistory(1L, 10L);

        assertThat(response.messages()).hasSize(1);
        assertThat(response.messages().get(0).referencesJson().get("sourceFields").get(0).asText())
                .isEqualTo("raw_result_json.scores.axes.vocabulary");
        verify(mentorMessageRepository)
                .findByMentorChatSessionAnalysisIdOrderByCreatedAtAscIdAsc(10L);
    }

    @Test
    @DisplayName("다른 사용자의 분석이면 접근 거부")
    void getMessageHistory_otherOwner_throwsAccessDenied() {
        given(analysisRepository.findUserIdById(10L)).willReturn(Optional.of(1L));

        assertThatThrownBy(() -> mentorService.getMessageHistory(2L, 10L))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", MentorErrorStatus.MENTOR_ACCESS_DENIED);

        verifyNoInteractions(mentorMessageRepository);
    }

    @Test
    @DisplayName("분석이 없으면 분석 미존재 오류")
    void getMessageHistory_analysisNotFound_throwsNotFound() {
        given(analysisRepository.findUserIdById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> mentorService.getMessageHistory(1L, 10L))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", AnalysisErrorStatus.ANALYSIS_NOT_FOUND);
    }

    @Test
    @DisplayName("판단 근거 JSON 하나가 손상되어도 나머지 대화 내역 반환")
    void getMessageHistory_invalidReferences_returnsNullAndContinues() {
        MentorMessage invalidMessage = mock(MentorMessage.class);
        MentorMessage validMessage = mock(MentorMessage.class);

        given(invalidMessage.getId()).willReturn(30L);
        given(invalidMessage.getRole()).willReturn(MessageRole.ASSISTANT);
        given(invalidMessage.getReferencesJson()).willReturn("{invalid");
        given(invalidMessage.getContent()).willReturn("손상된 판단 근거를 가진 답변");
        given(validMessage.getId()).willReturn(31L);
        given(validMessage.getRole()).willReturn(MessageRole.USER);
        given(validMessage.getContent()).willReturn("다음 질문");
        given(analysisRepository.findUserIdById(10L)).willReturn(Optional.of(1L));
        given(mentorMessageRepository.findByMentorChatSessionAnalysisIdOrderByCreatedAtAscIdAsc(10L))
                .willReturn(List.of(invalidMessage, validMessage));

        MentorMessageHistoryResponseDTO response = mentorService.getMessageHistory(1L, 10L);

        assertThat(response.messages()).hasSize(2);
        assertThat(response.messages().get(0).mentorMessageId()).isEqualTo(30L);
        assertThat(response.messages().get(0).referencesJson()).isNull();
        assertThat(response.messages().get(1).mentorMessageId()).isEqualTo(31L);
        assertThat(response.messages().get(1).content()).isEqualTo("다음 질문");
    }
}
