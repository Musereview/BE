package com.mr.domain.mentor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.repository.AnalysisReportRepository;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.domain.mentor.entity.MentorChatSession;
import com.mr.domain.mentor.entity.MentorMessage;
import com.mr.domain.mentor.exception.MentorErrorStatus;
import com.mr.domain.mentor.repository.MentorChatSessionRepository;
import com.mr.domain.mentor.repository.MentorMessageRepository;
import com.mr.domain.user.entity.User;
import com.mr.global.apipayload.exception.GeneralException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MentorQuestionServiceTest {

    private MentorQuestionService service;
    private AnalysisRepository analysisRepository;
    private AnalysisReportRepository analysisReportRepository;
    private MentorChatSessionRepository sessionRepository;
    private MentorMessageRepository messageRepository;

    @BeforeEach
    void setUp() {
        analysisRepository = mock(AnalysisRepository.class);
        analysisReportRepository = mock(AnalysisReportRepository.class);
        sessionRepository = mock(MentorChatSessionRepository.class);
        messageRepository = mock(MentorMessageRepository.class);
        service = new MentorQuestionService(
                analysisRepository,
                analysisReportRepository,
                sessionRepository,
                messageRepository,
                new ObjectMapper()
        );
    }

    @Test
    void prepare_rejectsBlankQuestionBeforeDatabaseAccess() {
        assertThatThrownBy(() -> service.prepare(1L, 10L, "  "))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", MentorErrorStatus.MENTOR_QUESTION_REQUIRED);
    }

    @Test
    void prepare_rejectsQuestionLongerThanFiveHundredCharacters() {
        assertThatThrownBy(() -> service.prepare(1L, 10L, "가".repeat(501)))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", MentorErrorStatus.MENTOR_QUESTION_TOO_LONG);
    }

    @Test
    void prepare_otherUsersAnalysis_throwsAccessDenied() {
        Analysis analysis = analysis(2L, AnalysisStatus.COMPLETED);
        given(analysisRepository.findByIdForUpdate(10L)).willReturn(Optional.of(analysis));

        assertThatThrownBy(() -> service.prepare(1L, 10L, "question"))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", MentorErrorStatus.MENTOR_ACCESS_DENIED);
        verify(sessionRepository, never()).findByAnalysisIdForUpdate(any());
    }

    @Test
    void prepare_incompleteAnalysis_throwsConflict() {
        Analysis analysis = analysis(1L, AnalysisStatus.PROCESSING);
        given(analysisRepository.findByIdForUpdate(10L)).willReturn(Optional.of(analysis));

        assertThatThrownBy(() -> service.prepare(1L, 10L, "question"))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", MentorErrorStatus.MENTOR_ANALYSIS_NOT_COMPLETED);
        verify(sessionRepository, never()).findByAnalysisIdForUpdate(any());
    }

    @Test
    void prepare_questionLimitExceeded_throwsTooManyRequests() {
        Analysis analysis = analysis(1L, AnalysisStatus.COMPLETED);
        User owner = analysis.getUser();
        MentorChatSession session = MentorChatSession.createActive(analysis, owner);
        session.increaseQuestionCount();
        session.increaseQuestionCount();
        session.increaseQuestionCount();
        given(analysisRepository.findByIdForUpdate(10L)).willReturn(Optional.of(analysis));
        given(sessionRepository.findByAnalysisIdForUpdate(10L)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> service.prepare(1L, 10L, "question"))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", MentorErrorStatus.MENTOR_QUESTION_LIMIT_EXCEEDED);
        verify(messageRepository, never()).saveAndFlush(any());
    }

    @Test
    void prepare_existingSession_reusesSession() {
        Analysis analysis = analysis(1L, AnalysisStatus.COMPLETED);
        MentorChatSession session = MentorChatSession.createActive(analysis, analysis.getUser());
        given(analysisRepository.findByIdForUpdate(10L)).willReturn(Optional.of(analysis));
        given(sessionRepository.findByAnalysisIdForUpdate(10L)).willReturn(Optional.of(session));
        given(analysisReportRepository.findFirstByAnalysisIdAndLlmStatusOrderByCreatedAtDesc(any(), any()))
                .willReturn(Optional.empty());
        given(messageRepository.findTop10ByMentorChatSessionAnalysisIdOrderByCreatedAtDescIdDesc(10L))
                .willReturn(List.of());
        given(messageRepository.saveAndFlush(any(MentorMessage.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        MentorQuestionService.PreparedQuestion prepared = service.prepare(1L, 10L, " question ");

        assertThat(prepared.generationToken()).isNotBlank();
        assertThat(prepared.prompt()).contains("\"question\":\"question\"");
        verify(sessionRepository, never()).save(any());
    }

    private Analysis analysis(Long ownerId, AnalysisStatus status) {
        User owner = mock(User.class);
        given(owner.getUserId()).willReturn(ownerId);
        Analysis analysis = mock(Analysis.class);
        given(analysis.getId()).willReturn(10L);
        given(analysis.getUser()).willReturn(owner);
        given(analysis.getStatus()).willReturn(status);
        given(analysis.getRawResultJson()).willReturn("{}");
        return analysis;
    }
}
