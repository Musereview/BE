package com.mr.domain.statistics.dto.req;

// 통계 화면 조회 쿼리 파라미터 검증용. MVP에서는 값과 무관하게 이번 주 vs 지난 주로 고정 집계
public enum StatisticsPeriod {
    WEEKLY,
    MONTHLY,
    RECENT_4_WEEKS
}
