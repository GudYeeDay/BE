package org.example.gudyeeday.domain.home.dto.response;

import org.example.gudyeeday.domain.complete.entity.CompleteMission;

import java.time.DayOfWeek;
import java.time.LocalDate;

public record WeeklyPhotoDayResponse(
        LocalDate date,
        // MONDAY ~ SUNDAY
        DayOfWeek dayOfWeek,
        // 오늘(KST) 여부
        boolean today,
        // 그날 기록 중 사진으로 쓴 기록. 그날 기록이 없으면 null
        Long completeMissionId,
        String imageUrl
) {
    public static WeeklyPhotoDayResponse of(LocalDate date, LocalDate today, CompleteMission record) {
        return new WeeklyPhotoDayResponse(
                date,
                date.getDayOfWeek(),
                date.equals(today),
                record != null ? record.getCompleteMissionId() : null,
                record != null ? record.getImageUrl() : null
        );
    }
}
