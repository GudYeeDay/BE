package org.example.gudyeeday.domain.mission.dto.response;

import org.example.gudyeeday.domain.mission.entity.Mission;
import org.example.gudyeeday.domain.mission.entity.UserMission;

public record InProgressMissionResponse(
        Long userMissionId,
        Long missionId,
        String title,
        String description
) {
    public static InProgressMissionResponse from(UserMission userMission) {
        Mission mission = userMission.getMission();
        return new InProgressMissionResponse(
                userMission.getId(),
                mission.getId(),
                mission.getTitle(),
                mission.getDescription()
        );
    }
}
