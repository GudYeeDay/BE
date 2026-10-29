package org.example.gudyeeday.domain.home.dto.response;

import java.util.List;

public record HomeResponse(
        // 사용자 닉네임
        String name,
        // 안 읽은 알림이 있는지
        boolean hasUnreadNotification,
        // 이번 달(KST) 미션을 완료한 날의 수 (하루에 여러 미션을 완료해도 1일)
        int monthlyCompletedDays,
        WeeklyPhotosResponse weeklyPhotos,
        // 이번 주 이전에 완료한 미션 기록 중 랜덤 최대 5개
        List<PastRecordResponse> pastRecords
) {
}
