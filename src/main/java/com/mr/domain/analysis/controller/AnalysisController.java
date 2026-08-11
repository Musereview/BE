package com.mr.domain.analysis.controller;

import com.mr.domain.analysis.dto.req.AnalysisCreateRequestDTO;
import com.mr.domain.analysis.dto.res.AnalysisCreateResponseDTO;
import com.mr.domain.analysis.dto.res.AnalysisResultResponseDTO;
import com.mr.domain.analysis.dto.res.AnalysisStatusResponseDTO;
import com.mr.domain.analysis.service.AnalysisService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analyses")
@Tag(name = "분석", description = "분석 API")
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping
    @Operation(
            summary = "분석 요청 생성 API",
            description = "완료된 연주의 특정 마디 구간에 대해 AI 분석을 비동기로 요청합니다. 요청 직후에는 PENDING 상태로 응답하며, 진행 상황은 분석 상태 조회 API로 확인합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "분석 요청 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "COMMON_200",
                                    summary = "분석 요청 생성 성공 예시",
                                    value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON_200",
                                      "message": "요청에 성공하였습니다.",
                                      "data": {
                                        "analysisId": 342,
                                        "playingId": 128,
                                        "status": "PENDING",
                                        "startBar": 1,
                                        "endBar": 8,
                                        "createdAt": "2026-08-10T21:20:05"
                                      }
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "COMMON_400_01",
                                            summary = "필수값 누락 또는 형식 오류",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "COMMON_400_01",
                                              "message": "입력값이 올바르지 않습니다.",
                                              "data": { "startBar": "must be greater than 0" }
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "ANALYSIS_400_01",
                                            summary = "시작 마디가 종료 마디보다 큼",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "ANALYSIS_400_01",
                                              "message": "분석 시작 마디는 종료 마디보다 클 수 없습니다.",
                                              "data": null
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "ANALYSIS_400_02",
                                            summary = "연주 전체 마디 수를 벗어난 구간 요청",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "ANALYSIS_400_02",
                                              "message": "분석 가능한 마디 범위를 벗어났습니다.",
                                              "data": null
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "ANALYSIS_400_03",
                                            summary = "백킹트랙 BPM/박자 등 분석에 필요한 정보 누락",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "ANALYSIS_400_03",
                                              "message": "필수 정보가 누락되었습니다.",
                                              "data": null
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "ANALYSIS_400_05",
                                            summary = "선택한 구간에 연주 노트가 없음",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "ANALYSIS_400_05",
                                              "message": "선택한 마디 범위에 분석할 연주 노트가 없습니다.",
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
                    description = "연주 접근 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "PLAYING_403_01",
                                    summary = "다른 사용자의 연주에 분석 요청 시도",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "PLAYING_403_01",
                                      "message": "해당 연주에 대한 접근 권한이 없습니다.",
                                      "data": null
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "연주 세션 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "PLAYING_404_01",
                                    summary = "존재하지 않는 연주 ID",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "PLAYING_404_01",
                                      "message": "연주 세션을 찾을 수 없습니다.",
                                      "data": null
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "완료되지 않은 연주",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "PLAYING_409_01",
                                    summary = "아직 완료되지 않은 연주에 분석 요청 시도",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "PLAYING_409_01",
                                      "message": "현재 연주 상태에서는 요청한 작업을 수행할 수 없습니다.",
                                      "data": null
                                    }
                                    """
                            )
                    )
            )
    })
    public ApiResponse<AnalysisCreateResponseDTO> createAnalysis(
            @Valid @RequestBody AnalysisCreateRequestDTO request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.onSuccess(analysisService.createAnalysis(userId, request));
    }

    @GetMapping("/{analysisId}/status")
    @Operation(
            summary = "분석 상태 조회 API",
            description = "분석 요청의 현재 처리 상태와 진행률을 조회합니다. 분석 완료 여부를 폴링할 때 사용합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "COMMON_200",
                                    summary = "분석 상태 조회 성공 예시",
                                    value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON_200",
                                      "message": "요청에 성공하였습니다.",
                                      "data": {
                                        "analysisId": 342,
                                        "status": "PROCESSING",
                                        "progressRate": null,
                                        "message": "연습 결과를 분석 중입니다.",
                                        "createdAt": "2026-08-10T21:20:05",
                                        "completedAt": null
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
                    description = "분석 접근 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "ANALYSIS_403_01",
                                    summary = "다른 사용자의 분석 조회 시도",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "ANALYSIS_403_01",
                                      "message": "해당 분석 결과에 접근할 수 없습니다.",
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
    public ApiResponse<AnalysisStatusResponseDTO> getAnalysisStatus(
            @Parameter(description = "조회할 분석 ID", example = "342")
            @PathVariable Long analysisId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        return ApiResponse.onSuccess(
                analysisService.getAnalysisStatus(userId, analysisId)
        );
    }

    @GetMapping("/{analysisId}")
    @Operation(
            summary = "분석 결과 조회 API",
            description = "완료된 분석의 점수·등급·영역별 점수와 생성된 리포트를 조회합니다. 분석이 완료되기 전에는 409로 응답합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "COMMON_200",
                                    summary = "분석 결과 조회 성공 예시",
                                    value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON_200",
                                      "message": "요청에 성공하였습니다.",
                                      "data": {
                                        "analysisId": 342,
                                        "playingId": 128,
                                        "title": "Autumn Leaves",
                                        "genre": "JAZZ",
                                        "key": "Bb Major",
                                        "bpm": 120,
                                        "playedAt": "2026-08-10T21:14:32",
                                        "recordingFileUrl": "https://bucket.s3.ap-northeast-2.amazonaws.com/recording/...",
                                        "backingTrackAudioFileUrl": "https://bucket.s3.ap-northeast-2.amazonaws.com/backing-track/...",
                                        "status": "COMPLETED",
                                        "startBar": 1,
                                        "endBar": 8,
                                        "totalScore": 82,
                                        "grade": "GOOD",
                                        "summary": "코드 전환은 안정적이나 8마디 이후 박자가 밀립니다.",
                                        "domainScores": {
                                          "scale": 85.5,
                                          "tension": 78.0,
                                          "progression": 88.0,
                                          "voiceLeading": 76.5
                                        },
                                        "report": {
                                          "analysisReportId": 91,
                                          "generationType": "LLM",
                                          "llmStatus": "SUCCESS",
                                          "contentFormat": "MARKDOWN",
                                          "content": "## 총평\\n8마디 구간에서 ...",
                                          "modelName": "gemini-2.5-flash",
                                          "promptVersion": "v1",
                                          "createdAt": "2026-08-10T21:21:40",
                                          "updatedAt": "2026-08-10T21:21:40"
                                        },
                                        "result": { "notes": [], "chords": [] },
                                        "createdAt": "2026-08-10T21:20:05",
                                        "completedAt": "2026-08-10T21:21:40"
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
                    description = "분석 접근 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "ANALYSIS_403_01",
                                    summary = "다른 사용자의 분석 조회 시도",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "ANALYSIS_403_01",
                                      "message": "해당 분석 결과에 접근할 수 없습니다.",
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
                    description = "완료되지 않은 분석",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "ANALYSIS_409_02",
                                    summary = "PENDING/PROCESSING/FAILED 상태의 분석 조회 시도",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "ANALYSIS_409_02",
                                      "message": "아직 완료되지 않은 분석입니다.",
                                      "data": null
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "저장된 분석 결과 처리 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "ANALYSIS_500_01",
                                    summary = "저장된 분석 결과 JSON 파싱 실패",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "ANALYSIS_500_01",
                                      "message": "저장된 분석 결과를 처리할 수 없습니다.",
                                      "data": null
                                    }
                                    """
                            )
                    )
            )
    })
    public ApiResponse<AnalysisResultResponseDTO> getAnalysisResult(
            @Parameter(description = "조회할 분석 ID", example = "342")
            @PathVariable Long analysisId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        return ApiResponse.onSuccess(
                analysisService.getAnalysisResult(userId, analysisId)
        );
    }
}
