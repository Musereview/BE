package com.mr.domain.statistics.controller;

import com.mr.domain.statistics.dto.res.StatisticsResponseDTO;
import com.mr.domain.statistics.service.StatisticsService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/statistics")
@Tag(name = "통계", description = "통계 API")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(
            summary = "통계 화면 조회 API",
            description = "현재 로그인한 사용자의 이번 주 요약, 지난주 대비 영역별 성장, 최근 4주 점수 추이를 조회합니다. "
                    + "조회 기간은 월요일 00시를 기준으로 고정되며 별도의 기간 쿼리 파라미터를 받지 않습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "COMMON_200",
                                    summary = "통계 화면 조회 성공 예시",
                                    value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON_200",
                                      "message": "요청에 성공하였습니다.",
                                      "data": {
                                        "weeklySummary": {
                                          "accuracy": 91.0,
                                          "practiceMinutes": 65,
                                          "completedSessionCount": 6,
                                          "accuracyDiff": 4.0,
                                          "practiceMinutesDiff": 18,
                                          "completedSessionCountDiff": 2
                                        },
                                        "domainGrowth": [
                                          { "domain": "SCALE", "label": "스케일", "currentScore": 85.0, "previousScore": 77.0, "diff": 8.0 },
                                          { "domain": "TENSION", "label": "텐션", "currentScore": 78.0, "previousScore": 80.0, "diff": -2.0 },
                                          { "domain": "PROGRESSION", "label": "진행", "currentScore": 88.0, "previousScore": 84.0, "diff": 4.0 },
                                          { "domain": "VOICE_LEADING", "label": "코드 연결", "currentScore": 0.0, "previousScore": 0.0, "diff": 0.0 }
                                        ],
                                        "weeklyTrend": {
                                          "diffFromPreviousWeek": 9,
                                          "items": [
                                            { "label": "3주 전", "averageScore": 0.0 },
                                            { "label": "2주 전", "averageScore": 79.0 },
                                            { "label": "지난주", "averageScore": 82.0 },
                                            { "label": "이번주", "averageScore": 91.0 }
                                          ]
                                        }
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
                    responseCode = "404",
                    description = "사용자 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "USER_404_01",
                                    summary = "탈퇴 등으로 존재하지 않는 사용자",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "USER_404_01",
                                      "message": "존재하지 않는 사용자입니다.",
                                      "data": null
                                    }
                                    """
                            )
                    )
            )
    })
    @GetMapping
    public ApiResponse<StatisticsResponseDTO> getStatistics() {
        Long userId = SecurityUtil.getCurrentUserId();

        return ApiResponse.onSuccess(
                statisticsService.getStatistics(userId)
        );
    }
}
