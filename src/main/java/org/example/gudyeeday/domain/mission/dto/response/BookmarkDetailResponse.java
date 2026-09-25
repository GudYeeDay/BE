package org.example.gudyeeday.domain.mission.dto.response;

import org.example.gudyeeday.domain.mission.entity.Mission;
import org.example.gudyeeday.domain.mission.entity.MissionBookmark;

public record BookmarkDetailResponse(
        Long bookmarkId,
        Long missionId,
        String title,
        String description,
        // 저장 날짜 (M.dd, KST)
        String savedDate,
        // 나만의 굳이 미션 여부
        boolean custom
) {
    public static BookmarkDetailResponse from(MissionBookmark bookmark) {
        Mission mission = bookmark.getMission();
        return new BookmarkDetailResponse(
                bookmark.getId(),
                mission.getId(),
                mission.getTitle(),
                mission.getDescription(),
                BookmarkDateFormatter.format(bookmark.getSavedAt().toLocalDate()),
                mission.isCustom()
        );
    }
}
