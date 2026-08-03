package com.mr.domain.mentor.entity;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.mentor.entity.enums.MentorChatStatus;
import com.mr.domain.mentor.exception.MentorErrorStatus;
import com.mr.domain.user.entity.User;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
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

    @Column(name = "generation_token", length = 36)
    private String generationToken;

    @Column(name = "generation_started_at")
    private LocalDateTime generationStartedAt;

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

    public void increaseQuestionCount() {
        validateActive();
        this.questionCount += 1;
    }

    public String startGenerating(Duration staleAfter) {
        if (this.status == MentorChatStatus.GENERATING && !isStale(staleAfter)) {
            throw new GeneralException(MentorErrorStatus.MENTOR_RESPONSE_IN_PROGRESS);
        }
        if (this.status != MentorChatStatus.ACTIVE && this.status != MentorChatStatus.GENERATING) {
            throw new GeneralException(MentorErrorStatus.MENTOR_SESSION_NOT_ACTIVE);
        }
        if (this.questionCount >= 3) {
            throw new GeneralException(MentorErrorStatus.MENTOR_QUESTION_LIMIT_EXCEEDED);
        }
        this.status = MentorChatStatus.GENERATING;
        this.generationToken = UUID.randomUUID().toString();
        this.generationStartedAt = LocalDateTime.now();
        return this.generationToken;
    }

    public void completeGenerating(String expectedToken) {
        if (this.status != MentorChatStatus.GENERATING
                || !Objects.equals(this.generationToken, expectedToken)) {
            throw new GeneralException(MentorErrorStatus.MENTOR_SESSION_NOT_ACTIVE);
        }
        this.questionCount += 1;
        this.lastMessageAt = LocalDateTime.now();
        this.status = MentorChatStatus.ACTIVE;
        clearGeneration();
    }

    public void failGenerating(String expectedToken) {
        if (this.status == MentorChatStatus.GENERATING
                && Objects.equals(this.generationToken, expectedToken)) {
            this.status = MentorChatStatus.ACTIVE;
            clearGeneration();
        }
    }

    private boolean isStale(Duration staleAfter) {
        return this.generationStartedAt == null
                || !this.generationStartedAt.isAfter(LocalDateTime.now().minus(staleAfter));
    }

    private void clearGeneration() {
        this.generationToken = null;
        this.generationStartedAt = null;
    }

    private void validateActive() {
        if (this.status != MentorChatStatus.ACTIVE) {
            throw new GeneralException(MentorErrorStatus.MENTOR_SESSION_NOT_ACTIVE);
        }
    }
}
