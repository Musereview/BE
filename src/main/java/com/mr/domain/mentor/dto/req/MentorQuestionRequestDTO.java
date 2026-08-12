package com.mr.domain.mentor.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 멘토 질문 전송 요청")
public record MentorQuestionRequestDTO(
        @Schema(description = "질문 내용 (공백 불가, 최대 500자)",
                example = "8마디 이후에 박자가 밀리는 이유가 뭔가요?",
                maxLength = 500,
                requiredMode = Schema.RequiredMode.REQUIRED)
        String content
) {
}
