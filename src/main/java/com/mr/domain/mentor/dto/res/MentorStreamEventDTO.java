package com.mr.domain.mentor.dto.res;

import com.fasterxml.jackson.databind.JsonNode;
import com.mr.domain.mentor.entity.MentorMessage;
import com.mr.domain.mentor.entity.enums.MessageRole;

import java.time.Instant;

public final class MentorStreamEventDTO {

    private MentorStreamEventDTO() {
    }

    public record Start(Long analysisId, Long mentorChatSessionId, Message userMessage) {
    }

    public record Chunk(String content) {
    }

    public record Complete(Message assistantMessage) {
    }

    public record Error(String code, String message) {
    }

    public record Message(Long mentorMessageId, MessageRole role, JsonNode referencesJson,
                          String content, Instant createdAt) {

        public static Message user(MentorMessage message) {
            return new Message(message.getId(), message.getRole(), null, message.getContent(), message.getCreatedAt());
        }

        public static Message assistant(MentorMessage message, JsonNode referencesJson) {
            return new Message(
                    message.getId(),
                    message.getRole(),
                    referencesJson,
                    message.getContent(),
                    message.getCreatedAt()
            );
        }
    }
}
