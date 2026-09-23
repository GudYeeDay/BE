package org.example.gudyeeday.domain.mission.dto.response;

import org.example.gudyeeday.domain.mission.entity.MissionBookmark;

public record MissionBookmarkResponse(
        Long bookmarkId,
        Long missionId
) {
    public static MissionBookmarkResponse from(MissionBookmark bookmark) {
        return new MissionBookmarkResponse(
                bookmark.getId(),
                bookmark.getMission().getId()
        );
    }
}
