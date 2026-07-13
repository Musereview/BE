package com.mr.global.client.ai;

// TODO: 분석 요청 생성 API 구현 시 AI 서버 실제 응답 스펙(raw_result_json 등)에 맞춰 필드 확정
public record AiAnalysisResponse(
        String rawResultJson
) {
}
