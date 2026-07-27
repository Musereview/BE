package com.mr.domain.mentor.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.mentor.exception.MentorErrorStatus;
import com.mr.domain.user.entity.User;
import com.mr.global.apipayload.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MentorChatSessionTest {

    @Test
    @DisplayName("createActive - 생성 시 questionCount는 0이다")
    void createActive_success_startsWithZeroQuestionCount() {
        MentorChatSession session = MentorChatSession.createActive(mock(Analysis.class), mock(User.class));

        assertThat(session.getQuestionCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("increaseQuestionCount - 활성 세션이면 카운트가 증가한다")
    void increaseQuestionCount_active_increments() {
        MentorChatSession session = MentorChatSession.createActive(mock(Analysis.class), mock(User.class));

        session.increaseQuestionCount();

        assertThat(session.getQuestionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("increaseQuestionCount - 닫힌 세션이면 예외가 발생한다")
    void increaseQuestionCount_closedSession_throwsException() {
        MentorChatSession session = MentorChatSession.createActive(mock(Analysis.class), mock(User.class));
        session.close();

        assertThatThrownBy(session::increaseQuestionCount)
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", MentorErrorStatus.MENTOR_SESSION_NOT_ACTIVE);
    }
}
