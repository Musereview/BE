package com.mr.domain.mentor.dto.res;

import com.fasterxml.jackson.databind.JsonNode;
import com.mr.domain.mentor.entity.MentorMessage;
import com.mr.domain.mentor.entity.enums.MessageRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "AI 멘토 대화 내역 조회 응답")
public record MentorMessageHistoryResponseDTO(
        @Schema(description = "대화가 연결된 분석 ID", example = "342")
        Long analysisId,

        @Schema(description = "대화 메시지 목록 (생성 시각 오름차순). 대화 이력이 없으면 빈 배열")
        List<Message> messages
) {

    @Schema(description = "AI 멘토 대화 메시지")
    public record Message(
            @Schema(description = "메시지 ID", example = "501")
            Long mentorMessageId,

            @Schema(description = "메시지 작성 주체", example = "USER")
            MessageRole role,

            @Schema(description = "AI 답변이 참고한 데이터 출처(JSON). 사용자 메시지이거나 출처가 없으면 null")
            JsonNode referencesJson,

            @Schema(description = "메시지 본문", example = "8마디 이후에 박자가 밀리는 이유가 뭔가요?")
            String content,

            @Schema(description = "메시지 생성 시각", example = "2026-08-10T21:30:12")
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
