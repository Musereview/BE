package com.mr.domain.mentor.controller;

import com.mr.domain.mentor.dto.req.MentorQuestionRequestDTO;
import com.mr.domain.mentor.dto.res.MentorMessageHistoryResponseDTO;
import com.mr.domain.mentor.service.MentorQuestionService;
import com.mr.domain.mentor.service.MentorService;
import com.mr.domain.mentor.service.MentorStreamingService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analyses")
@Tag(name = "AI 멘토", description = "AI 멘토 API")
public class MentorController {

    private final MentorService mentorService;
    private final MentorQuestionService mentorQuestionService;
    private final MentorStreamingService mentorStreamingService;

    @GetMapping("/{analysisId}/mentor/messages")
    @Operation(
            summary = "AI 멘토 대화 내역 조회 API",
            description = "분석 결과에 연결된 AI 멘토 대화를 오래된 순으로 조회합니다. 아직 대화한 적이 없으면 빈 배열을 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "COMMON_200",
                                    summary = "AI 멘토 대화 내역 조회 성공 예시",
                                    value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON_200",
                                      "message": "요청에 성공하였습니다.",
                                      "data": {
                                        "analysisId": 342,
                                        "messages": [
                                          {
                                            "mentorMessageId": 501,
                                            "role": "USER",
                                            "referencesJson": null,
                                            "content": "8마디 이후에 박자가 밀리는 이유가 뭔가요?",
                                            "createdAt": "2026-08-10T21:30:12"
                                          },
                                          {
                                            "mentorMessageId": 502,
                                            "role": "ASSISTANT",
                                            "referencesJson": {
                                              "sourceFields": ["analysis.raw_result_json", "analysis_report.content"]
                                            },
                                            "content": "코드가 바뀌는 지점에서 왼손 이동이 늦어지는 패턴이 보입니다. ...",
                                            "createdAt": "2026-08-10T21:30:25"
                                          }
                                        ]
                                      }
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (로그인 토큰 누락/만료)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "COMMON_401_01",
                                    summary = "인증 토큰 누락/만료",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON_401_01",
                                      "message": "인증이 필요합니다.",
                                      "data": null
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "대화 접근 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "MENTOR_403_01",
                                    summary = "다른 사용자의 분석에 연결된 대화 조회 시도",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "MENTOR_403_01",
                                      "message": "해당 대화에 접근할 수 없습니다.",
                                      "data": null
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "분석 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "ANALYSIS_404_01",
                                    summary = "존재하지 않는 분석 ID",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "ANALYSIS_404_01",
                                      "message": "분석 결과를 찾을 수 없습니다.",
                                      "data": null
                                    }
                                    """
                            )
                    )
            )
    })
    public ApiResponse<MentorMessageHistoryResponseDTO> getMessageHistory(
            @Parameter(description = "대화를 조회할 분석 ID", example = "342")
            @PathVariable Long analysisId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.onSuccess(mentorService.getMessageHistory(userId, analysisId));
    }

    @PostMapping(
            value = "/{analysisId}/mentor/messages",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    @Operation(
            summary = "AI 멘토 질문 전송 API",
            description = "질문을 저장하고 AI 답변을 SSE(text/event-stream)로 실시간 전송합니다. "
                    + "이벤트는 start(질문 저장 완료) → chunk(답변 조각 반복) → complete(답변 저장 완료) 순으로 전달되며, "
                    + "생성 중 오류가 나면 complete 대신 error 이벤트가 전달됩니다. "
                    + "질문 검증/권한 검증 실패는 스트림이 열리기 전에 발생하므로 SSE가 아닌 일반 JSON 에러 응답으로 내려갑니다. "
                    + "스트림 타임아웃은 70초입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "SSE 스트림 시작",
                    content = @Content(
                            mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                            examples = @ExampleObject(
                                    name = "SSE_STREAM",
                                    summary = "start → chunk → complete 이벤트 예시",
                                    value = """
                                    event: start
                                    data: {"analysisId":342,"mentorChatSessionId":77,"userMessage":{"mentorMessageId":501,"role":"USER","referencesJson":null,"content":"8마디 이후에 박자가 밀리는 이유가 뭔가요?","createdAt":"2026-08-10T21:30:12"}}

                                    event: chunk
                                    data: {"content":"코드가 바뀌는 지점에서 "}

                                    event: chunk
                                    data: {"content":"왼손 이동이 늦어지는 패턴이 보입니다."}

                                    event: complete
                                    data: {"assistantMessage":{"mentorMessageId":502,"role":"ASSISTANT","referencesJson":{"sourceFields":["analysis.raw_result_json","analysis_report.content"]},"content":"코드가 바뀌는 지점에서 왼손 이동이 늦어지는 패턴이 보입니다.","createdAt":"2026-08-10T21:30:25"}}
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 질문 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "MENTOR_400_01",
                                            summary = "질문 내용이 비어 있음",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "MENTOR_400_01",
                                              "message": "질문 내용을 입력해주세요.",
                                              "data": null
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "MENTOR_400_02",
                                            summary = "질문이 500자를 초과함",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "MENTOR_400_02",
                                              "message": "질문은 500자 이하로 입력해주세요.",
                                              "data": null
                                            }
                                            """
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (로그인 토큰 누락/만료)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "COMMON_401_01",
                                    summary = "인증 토큰 누락/만료",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON_401_01",
                                      "message": "인증이 필요합니다.",
                                      "data": null
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "대화 접근 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "MENTOR_403_01",
                                    summary = "다른 사용자의 분석에 질문 시도",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "MENTOR_403_01",
                                      "message": "해당 대화에 접근할 수 없습니다.",
                                      "data": null
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "분석 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "ANALYSIS_404_01",
                                    summary = "존재하지 않는 분석 ID",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "ANALYSIS_404_01",
                                      "message": "분석 결과를 찾을 수 없습니다.",
                                      "data": null
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "질문할 수 없는 상태",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "MENTOR_409_01",
                                            summary = "분석이 완료되지 않음",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "MENTOR_409_01",
                                              "message": "분석 완료 후 질문할 수 있습니다.",
                                              "data": null
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "MENTOR_409_02",
                                            summary = "직전 질문의 답변을 아직 생성 중 (2분 내 재요청)",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "MENTOR_409_02",
                                              "message": "이미 AI 멘토 답변을 생성하고 있습니다.",
                                              "data": null
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "MENTOR_409_03",
                                            summary = "종료된 대화 세션에 질문 시도",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "MENTOR_409_03",
                                              "message": "활성 상태의 세션에서만 질문할 수 있습니다.",
                                              "data": null
                                            }
                                            """
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "답변 생성/저장 실패. 스트림이 열린 뒤 발생하면 이 JSON 대신 동일한 code를 담은 SSE error 이벤트로 전달됩니다.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "MENTOR_500_01",
                                            summary = "AI 답변 생성 실패 또는 스트림 타임아웃",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "MENTOR_500_01",
                                              "message": "AI 멘토 답변 생성에 실패했습니다.",
                                              "data": null
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "MENTOR_500_02",
                                            summary = "생성된 답변 저장 실패",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "MENTOR_500_02",
                                              "message": "멘토 대화 저장에 실패했습니다.",
                                              "data": null
                                            }
                                            """
                                    )
                            }
                    )
            )
    })
    public SseEmitter sendQuestion(
            @Parameter(description = "질문할 분석 ID", example = "342")
            @PathVariable Long analysisId,

            @RequestBody MentorQuestionRequestDTO request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        MentorQuestionService.PreparedQuestion prepared =
                mentorQuestionService.prepare(userId, analysisId, request.content());
        return mentorStreamingService.stream(prepared);
    }
}
