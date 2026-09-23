package org.example.gudyeeday.domain.mission.dto.response;

import org.example.gudyeeday.domain.mission.entity.Mission;

public record MissionRecommendResponse(
        Long missionId,
        String title,
        String description
) {
    public static MissionRecommendResponse from(Mission mission) {
        return new MissionRecommendResponse(
                mission.getId(),
                mission.getTitle(),
                mission.getDescription()
        );
    }
}
