package com.mr.global.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public final class RelativeDateFormatter {

    private RelativeDateFormatter() {
    }

    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        long daysBetween = ChronoUnit.DAYS.between(dateTime.toLocalDate(), LocalDate.now());

        if (daysBetween <= 0) {
            return "오늘";
        }
        if (daysBetween == 1) {
            return "어제";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("M월 d일"));
    }
}
