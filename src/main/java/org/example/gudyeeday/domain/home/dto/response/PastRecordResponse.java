package org.example.gudyeeday.domain.home.dto.response;

import org.example.gudyeeday.domain.complete.entity.CompleteMission;
import org.example.gudyeeday.domain.mission.entity.Mission;
import org.example.gudyeeday.domain.mission.entity.UserMission;

import java.time.LocalDate;

public record PastRecordResponse(
        Long completeMissionId,
        // 기록 사진
        String imageUrl,
        // 미션 이름
        String title,
        // 미션 설명 (기록 내용 아님)
        String description,
        // 미션 완료 날짜
        LocalDate date,
        // 기록한 위치, 입력하지 않았으면 null
        String location
) {
    public static PastRecordResponse from(CompleteMission completeMission) {
        UserMission userMission = completeMission.getUserMission();
        Mission mission = userMission.getMission();
        return new PastRecordResponse(
                completeMission.getCompleteMissionId(),
                completeMission.getImageUrl(),
                mission.getTitle(),
                mission.getDescription(),
                userMission.getCompletedAt().toLocalDate(),
                completeMission.getLocation()
        );
    }
}
