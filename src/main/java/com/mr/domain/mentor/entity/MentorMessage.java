package com.mr.domain.mentor.entity;

import com.mr.domain.mentor.entity.enums.MessageRole;
import com.mr.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
        name = "mentor_message",
        indexes = {
                @Index(
                        name = "idx_mentor_message_session_created_id",
                        columnList = "mentor_chat_session_id, created_at, mentor_message_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MentorMessage extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mentor_message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_chat_session_id", nullable = false)
    private MentorChatSession mentorChatSession;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MessageRole role;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "references_json", columnDefinition = "json")
    private String referencesJson;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    private MentorMessage(MentorChatSession mentorChatSession, MessageRole role, String referencesJson,
                          String content) {
        this.mentorChatSession = mentorChatSession;
        this.role = role;
        this.referencesJson = referencesJson;
        this.content = content;
    }

    public static MentorMessage createUserMessage(MentorChatSession mentorChatSession, String content) {
        return new MentorMessage(
                mentorChatSession,
                MessageRole.USER,
                null,
                content
        );
    }

    public static MentorMessage createAssistantMessage(MentorChatSession mentorChatSession,
                                                       String referencesJson, String content) {
        return new MentorMessage(
                mentorChatSession,
                MessageRole.ASSISTANT,
                referencesJson,
                content
        );
    }
}
