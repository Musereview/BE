package com.mr.domain.mentor.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.analysis.repository.AnalysisReportRepository;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.domain.mentor.exception.MentorErrorStatus;
import com.mr.domain.mentor.repository.MentorChatSessionRepository;
import com.mr.domain.mentor.repository.MentorMessageRepository;
import com.mr.global.apipayload.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MentorQuestionServiceTest {

    private MentorQuestionService service;

    @BeforeEach
    void setUp() {
        service = new MentorQuestionService(
                mock(AnalysisRepository.class),
                mock(AnalysisReportRepository.class),
                mock(MentorChatSessionRepository.class),
                mock(MentorMessageRepository.class),
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
}
