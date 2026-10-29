package org.example.gudyeeday.domain.home.dto.response;

import org.example.gudyeeday.domain.home.entity.WeeklyPhoto;

import java.time.DayOfWeek;
import java.time.LocalDate;

public record WeeklyPhotoDayResponse(
        LocalDate date,
        // MONDAY ~ SUNDAY
        DayOfWeek dayOfWeek,
        // 오늘(KST) 여부. 사진은 오늘 날짜에만 올릴 수 있음
        boolean today,
        // 그날 올린 사진이 없으면 null
        Long weeklyPhotoId,
        String imageUrl
) {
    public static WeeklyPhotoDayResponse of(LocalDate date, LocalDate today, WeeklyPhoto weeklyPhoto) {
        return new WeeklyPhotoDayResponse(
                date,
                date.getDayOfWeek(),
                date.equals(today),
                weeklyPhoto != null ? weeklyPhoto.getId() : null,
                weeklyPhoto != null ? weeklyPhoto.getImageUrl() : null
        );
    }
}
