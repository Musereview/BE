package com.mr.domain.mentor.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.mr.domain.mentor.entity.MentorMessage;
import com.mr.domain.mentor.entity.enums.MessageRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public final class MentorStreamEventDTO {

    private MentorStreamEventDTO() {
    }

    @Schema(description = "SSE start 이벤트. 질문 저장이 끝나고 답변 생성이 시작될 때 1회 전달")
    public record Start(
            @Schema(description = "질문이 연결된 분석 ID", example = "342")
            Long analysisId,

            @Schema(description = "대화 세션 ID", example = "77")
            Long mentorChatSessionId,

            @Schema(description = "저장된 사용자 질문 메시지")
            Message userMessage
    ) {
    }

    @Schema(description = "SSE chunk 이벤트. 생성 중인 답변 조각이 순차로 반복 전달")
    public record Chunk(
            @Schema(description = "답변 조각", example = "코드가 바뀌는 지점에서 ")
            String content
    ) {
    }

    @Schema(description = "SSE complete 이벤트. 답변 저장이 끝났을 때 1회 전달")
    public record Complete(
            @Schema(description = "저장된 AI 답변 메시지")
            Message assistantMessage
    ) {
    }

    @Schema(description = "SSE error 이벤트. 답변 생성/저장 실패 시 complete 대신 전달")
    public record Error(
            @Schema(description = "에러 코드", example = "MENTOR_500_01")
            String code,

            @Schema(description = "에러 메시지", example = "AI 멘토 답변 생성에 실패했습니다.")
            String message
    ) {
    }

    @Schema(description = "SSE 이벤트에 담기는 대화 메시지")
    public record Message(
            @Schema(description = "메시지 ID", example = "501")
            Long mentorMessageId,

            @Schema(description = "메시지 작성 주체", example = "USER")
            MessageRole role,

            @Schema(description = "AI 답변이 참고한 데이터 출처(JSON). 사용자 메시지면 null")
            JsonNode referencesJson,

            @Schema(description = "메시지 본문", example = "8마디 이후에 박자가 밀리는 이유가 뭔가요?")
            String content,

            @Schema(description = "메시지 생성 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
            Instant createdAt
    ) {

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