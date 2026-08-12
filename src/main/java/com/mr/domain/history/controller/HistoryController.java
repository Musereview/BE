package com.mr.domain.history.controller;

import com.mr.domain.history.dto.req.HistoryPeriod;
import com.mr.domain.history.dto.res.HistoryDetailResponseDTO;
import com.mr.domain.history.dto.res.HistoryListResponseDTO;
import com.mr.domain.history.service.HistoryService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "히스토리", description = "히스토리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/histories")
public class HistoryController {

    private final HistoryService historyService;

    @Operation(
            summary = "연주 히스토리 목록 조회 API",
            description = "완료된 연주 기록을 최신순으로 페이징 조회합니다. 각 항목에는 최근 완료된 분석의 요약과 직전 연주 대비 점수 변화가 함께 내려갑니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "COMMON_200",
                                    summary = "히스토리 목록 조회 성공 예시",
                                    value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON_200",
                                      "message": "요청에 성공하였습니다.",
                                      "data": {
                                        "page": 0,
                                        "size": 10,
                                        "hasNext": true,
                                        "items": [
                                          {
                                            "playingId": 128,
                                            "latestAnalysisId": 342,
                                            "title": "Autumn Leaves",
                                            "summary": "코드 전환은 안정적이나 8마디 이후 박자가 밀립니다.",
                                            "scoreChange": 5,
                                            "durationMinutes": 3,
                                            "durationSec": 215,
                                            "playedAt": "2026-08-10T21:14:32",
                                            "relativeDate": "어제"
                                          }
                                        ]
                                      }
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 파라미터",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "HISTORY_400_01",
                                            summary = "페이지 정보가 유효 범위를 벗어남 (page < 0 또는 size 1~50 밖)",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "HISTORY_400_01",
                                              "message": "요청한 페이지 정보가 올바르지 않습니다.",
                                              "data": null
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "COMMON_400_01",
                                            summary = "지원하지 않는 조회 기간 값",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "COMMON_400_01",
                                              "message": "입력값이 올바르지 않습니다.",
                                              "data": { "period": "요청 파라미터 형식이 올바르지 않습니다." }
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
            )
    })
    @GetMapping
    public ApiResponse<HistoryListResponseDTO> getHistories(
            @Parameter(description = "조회할 페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지당 조회 개수 (1~50)", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "조회 기간 필터. 미입력 또는 RECENT는 전체 기간 조회 (WEEKLY: 최근 7일, MONTHLY: 최근 30일)", example = "WEEKLY")
            @RequestParam(required = false) HistoryPeriod period
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        return ApiResponse.onSuccess(
                historyService.getHistories(userId, page, size, period)
        );
    }

    @Operation(
            summary = "연주 히스토리 상세 조회 API",
            description = "완료된 연주 기록 단건의 상세 정보(백킹트랙 정보, 녹음/백킹트랙 다운로드 URL, MIDI 이벤트, 분석 목록)를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "COMMON_200",
                                    summary = "히스토리 상세 조회 성공 예시",
                                    value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON_200",
                                      "message": "요청에 성공하였습니다.",
                                      "data": {
                                        "playingId": 128,
                                        "title": "Autumn Leaves",
                                        "genre": "JAZZ",
                                        "key": "Bb",
                                        "bpm": 120,
                                        "timeSignature": "4/4",
                                        "playedAt": "2026-08-10T21:14:32",
                                        "durationMinutes": 3,
                                        "durationSec": 215,
                                        "recordingFileUrl": "https://bucket.s3.ap-northeast-2.amazonaws.com/recording/...",
                                        "backingTrackAudioFileUrl": "https://bucket.s3.ap-northeast-2.amazonaws.com/backing-track/...",
                                        "midiEvents": [
                                          { "sequence": 1, "type": "NOTE_ON", "pitch": 60, "velocity": 90, "timestampMs": 1200 },
                                          { "sequence": 2, "type": "NOTE_OFF", "pitch": 60, "velocity": 0, "timestampMs": 1650 }
                                        ],
                                        "backingTrackMidiData": null,
                                        "totalBars": 32,
                                        "analyses": [
                                          {
                                            "analysisId": 342,
                                            "startBar": 1,
                                            "endBar": 8,
                                            "title": "1마디-8마디 분석 리포트",
                                            "oneLineSummary": "코드 전환은 안정적이나 8마디 이후 박자가 밀립니다.",
                                            "status": "COMPLETED",
                                            "estimatedSeconds": 16,
                                            "createdAt": "2026-08-10T21:20:05"
                                          }
                                        ]
                                      }
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 파라미터",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "HISTORY_400_02",
                                    summary = "연주 히스토리 ID가 1 미만",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "HISTORY_400_02",
                                      "message": "연주 히스토리 ID가 올바르지 않습니다.",
                                      "data": null
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
                    description = "조회 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "HISTORY_403_01",
                                    summary = "다른 사용자의 연주 기록 조회 시도",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "HISTORY_403_01",
                                      "message": "연주 히스토리 조회 권한이 없습니다.",
                                      "data": null
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "연주 히스토리 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "HISTORY_404_01",
                                    summary = "존재하지 않는 연주 히스토리 ID",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "HISTORY_404_01",
                                      "message": "연주 히스토리를 찾을 수 없습니다.",
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
                                    name = "HISTORY_409_01",
                                    summary = "아직 진행 중이거나 중단된 연주 조회 시도",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "HISTORY_409_01",
                                      "message": "완료된 연주 히스토리만 조회할 수 있습니다.",
                                      "data": null
                                    }
                                    """
                            )
                    )
            )
    })
    @GetMapping("/{playingId}")
    public ApiResponse<HistoryDetailResponseDTO> getHistoryDetail(
            @Parameter(description = "조회할 연주 기록 ID", example = "128")
            @PathVariable Long playingId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        return ApiResponse.onSuccess(
                historyService.getHistoryDetail(userId, playingId)
        );
    }
}
