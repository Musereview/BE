package com.mr.domain.mentor.entity;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.mentor.entity.enums.MentorChatStatus;
import com.mr.domain.mentor.exception.MentorErrorStatus;
import com.mr.domain.user.entity.User;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "mentor_chat_sessions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_mentor_chat_sessions_analysis_id",
                        columnNames = "analysis_id"
                )
        },
        indexes = {
                @Index(name = "idx_mentor_chat_sessions_user_id", columnList = "user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MentorChatSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mentor_chat_session_id")
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MentorChatStatus status;

    @Column(name = "disabled_reason", length = 50)
    private String disabledReason;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "question_count", nullable = false)
    private Integer questionCount;

    private MentorChatSession(Analysis analysis, User user, MentorChatStatus status,
                              String disabledReason, LocalDateTime lastMessageAt) {
        validateAnalysis(analysis);
        validateUser(user);

        this.analysis = analysis;
        this.user = user;
        this.status = status;
        this.disabledReason = disabledReason;
        this.lastMessageAt = lastMessageAt;
        this.questionCount = 0;
    }

    private static void validateAnalysis(Analysis analysis) {
        if (analysis == null) {
            throw new GeneralException(MentorErrorStatus.MENTOR_INVALID_REQUEST);
        }
    }

    private static void validateUser(User user) {
        if (user == null) {
            throw new GeneralException(MentorErrorStatus.MENTOR_INVALID_REQUEST);
        }
    }

    public static MentorChatSession createActive(Analysis analysis, User user) {
        return new MentorChatSession(
                analysis,
                user,
                MentorChatStatus.ACTIVE,
                null,
                null
        );
    }

    public void close() {
        this.status = MentorChatStatus.CLOSED;
    }

    public void disable(String disabledReason) {
        this.status = MentorChatStatus.DISABLED;
        this.disabledReason = disabledReason;
    }

    public void updateLastMessageAt() {
        this.lastMessageAt = LocalDateTime.now();
    }

    // TODO: 질문 횟수 3회 제한(MENTOR_429_01) 체크는 질문 전송 API 구현 시 추가
    public void increaseQuestionCount() {
        validateActive();
        this.questionCount += 1;
    }

    private void validateActive() {
        if (this.status != MentorChatStatus.ACTIVE) {
            throw new GeneralException(MentorErrorStatus.MENTOR_SESSION_NOT_ACTIVE);
        }
    }
}
