package org.example.gudyeeday.domain.mypage.dto.response;

public record ProfileResponse(
        String name,
        Long date,
        Long record,
        Long complete,
        Long left
) {
}
