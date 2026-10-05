package org.example.gudyeeday.domain.home.dto.response;

import org.example.gudyeeday.domain.home.entity.WeeklyPhoto;

import java.time.LocalDate;

public record WeeklyPhotoResponse(
        Long weeklyPhotoId,
        // 사진을 올린 날짜 (KST)
        LocalDate date,
        String imageUrl
) {
    public static WeeklyPhotoResponse from(WeeklyPhoto weeklyPhoto) {
        return new WeeklyPhotoResponse(
                weeklyPhoto.getId(),
                weeklyPhoto.getPhotoDate(),
                weeklyPhoto.getImageUrl()
        );
    }
}
