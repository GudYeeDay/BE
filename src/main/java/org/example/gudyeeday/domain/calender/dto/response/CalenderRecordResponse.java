package org.example.gudyeeday.domain.calender.dto.response;

import org.example.gudyeeday.domain.complete.entity.CompleteMission;

import java.time.LocalDate;

public record CalenderRecordResponse(
        Long CompleteMissionId,
        LocalDate CompleteDate,
        String ImageUrl
) {
    public static CalenderRecordResponse from(CompleteMission completeMission) {
        return new CalenderRecordResponse(
                completeMission.getCompleteMissionId(),
                completeMission.getUserMission().getCompletedAt().toLocalDate(),
                completeMission.getImageUrl()
        );
    }
}
