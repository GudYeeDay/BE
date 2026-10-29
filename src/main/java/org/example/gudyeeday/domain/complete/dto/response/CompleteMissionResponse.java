package org.example.gudyeeday.domain.complete.dto.response;

import org.example.gudyeeday.domain.complete.entity.CompleteMission;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CompleteMissionResponse(
        Long completeMissionId,
        String imageUrl,
        String location,
        String title,
        LocalDate completeAt,
        String content
) {
    public static CompleteMissionResponse from(CompleteMission completeMission) {
        return new CompleteMissionResponse(
                completeMission.getCompleteMissionId(),
                completeMission.getImageUrl(),
                completeMission.getLocation(),
                completeMission.getUserMission().getMission().getTitle(),
                completeMission.getUserMission().getCompletedAt().toLocalDate(),
                completeMission.getContent()
        );
    }
}
