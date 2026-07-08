package com.mr.domain.mentor.entity;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.mentor.entity.enums.MentorChatStatus;
import com.mr.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "mentor_chat_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MentorChatSession extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mentor_chat_session_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    /**
     * TODO: User 도메인 엔티티 연관관계 연결 예정
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MentorChatStatus status;

    @Column(name = "disabled_reason", length = 50)
    private String disabledReason;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    private MentorChatSession(Analysis analysis, Long userId, MentorChatStatus status,
                              String disabledReason, LocalDateTime lastMessageAt) {
        this.analysis = analysis;
        this.userId = userId;
        this.status = status;
        this.disabledReason = disabledReason;
        this.lastMessageAt = lastMessageAt;
    }

    public static MentorChatSession createActive(Analysis analysis, Long userId) {
        return new MentorChatSession(
                analysis,
                userId,
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
}
