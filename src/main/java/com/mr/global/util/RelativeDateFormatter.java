package com.mr.global.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public final class RelativeDateFormatter {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul"); // 항상 KST(한국 시간)를 기준으로 계산하도록 고정
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("M월 d일");

    private RelativeDateFormatter() {
    }

    public static String format(Instant dateTime) {
        if (dateTime == null) {
            return null;
        }

        LocalDate targetDate = dateTime.atZone(KST_ZONE).toLocalDate(); // Instant를 KST 기준의 달력 날짜(LocalDate)로 변환
        LocalDate today = LocalDate.now(KST_ZONE);

        long daysBetween = ChronoUnit.DAYS.between(targetDate, today);

        if (daysBetween <= 0) {
            return "오늘";
        }
        if (daysBetween == 1) {
            return "어제";
        }

        return targetDate.format(DATE_FORMATTER);
    }
}