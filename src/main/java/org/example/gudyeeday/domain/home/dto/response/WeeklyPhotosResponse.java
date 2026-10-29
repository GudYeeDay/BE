package org.example.gudyeeday.domain.home.dto.response;

import java.time.LocalDate;
import java.util.List;

public record WeeklyPhotosResponse(
        // 이번 주 월요일 (KST)
        LocalDate startDate,
        // 이번 주 일요일 (KST)
        LocalDate endDate,
        // 월요일부터 일요일까지 7일
        List<WeeklyPhotoDayResponse> days
) {
}
