package com.mr.domain.mentor.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.mentor.exception.MentorErrorStatus;
import com.mr.domain.user.entity.User;
import com.mr.global.apipayload.exception.GeneralException;
import java.time.Duration;
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

    @Test
    @DisplayName("생성 토큰이 일치할 때만 답변 완료 처리")
    void completeGenerating_requiresMatchingToken() {
        MentorChatSession session = MentorChatSession.createActive(mock(Analysis.class), mock(User.class));
        String token = session.startGenerating(Duration.ofMinutes(2));

        assertThatThrownBy(() -> session.completeGenerating("other-token"))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", MentorErrorStatus.MENTOR_SESSION_NOT_ACTIVE);

        session.completeGenerating(token);

        assertThat(session.getQuestionCount()).isEqualTo(1);
        assertThat(session.getGenerationToken()).isNull();
    }

    @Test
    @DisplayName("진행 중인 생성 요청은 중복 시작할 수 없다")
    void startGenerating_inProgress_throwsException() {
        MentorChatSession session = MentorChatSession.createActive(mock(Analysis.class), mock(User.class));
        session.startGenerating(Duration.ofMinutes(2));

        assertThatThrownBy(() -> session.startGenerating(Duration.ofMinutes(2)))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", MentorErrorStatus.MENTOR_RESPONSE_IN_PROGRESS);
    }

    @Test
    @DisplayName("stale GENERATING 세션은 새 생성 토큰으로 복구한다")
    void startGenerating_staleGeneration_issuesNewToken() {
        MentorChatSession session = MentorChatSession.createActive(mock(Analysis.class), mock(User.class));
        String previousToken = session.startGenerating(Duration.ofMinutes(2));

        String newToken = session.startGenerating(Duration.ZERO);

        assertThat(newToken).isNotEqualTo(previousToken);
        assertThat(session.getGenerationToken()).isEqualTo(newToken);
    }
}
