package org.example.gudyeeday.domain.mission.dto.response;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

// 보관함 날짜 표기: 월.일 (예: 9.02, 12.25)
final class BookmarkDateFormatter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("M.dd");

    private BookmarkDateFormatter() {
    }

    static String format(LocalDate date) {
        return date.format(FORMATTER);
    }
}
