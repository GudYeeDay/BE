package org.example.gudyeeday.domain.home.dto.response;

public record HomeResponse(
        // 사용자 닉네임
        String name,
        // 이번 달(KST) 미션을 완료한 날의 수 (하루에 여러 미션을 완료해도 1일)
        int monthlyCompletedDays,
        WeeklyPhotosResponse weeklyPhotos
) {
}
