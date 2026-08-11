package com.mr.domain.mentor.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.mr.domain.mentor.entity.MentorMessage;
import com.mr.domain.mentor.entity.enums.MessageRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
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

            @Schema(description = "메시지 생성 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
            Instant createdAt
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