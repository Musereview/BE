package com.mr.domain.mentor.dto.res;

import com.fasterxml.jackson.databind.JsonNode;
import com.mr.domain.mentor.entity.MentorMessage;
import com.mr.domain.mentor.entity.enums.MessageRole;
import java.time.LocalDateTime;
import java.util.List;

public record MentorMessageHistoryResponseDTO(
        Long analysisId,
        List<Message> messages
) {

    public record Message(
            Long mentorMessageId,
            MessageRole role,
            JsonNode referencesJson,
            String content,
            LocalDateTime createdAt
    ) {

        public static Message from(MentorMessage message, JsonNode referencesJson) {
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
