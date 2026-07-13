package com.mr.global.client.ai;

// TODO: 분석 요청 생성 API 구현 시 AI 서버 실제 요청 스펙에 맞춰 필드 확정
public record AiAnalysisRequest(
        Long playingId,
        Integer startBar,
        Integer endBar
) {
}
