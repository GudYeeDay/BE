package org.example.gudyeeday.domain.mission.dto.response;

import org.example.gudyeeday.domain.mission.entity.Mission;
import org.example.gudyeeday.domain.mission.entity.MissionBookmark;
import org.example.gudyeeday.domain.mission.enums.BookmarkMissionStatus;

import java.time.LocalDate;

public record BookmarkItemResponse(
        Long bookmarkId,
        Long missionId,
        String title,
        // SAVED: 저장만 함, IN_PROGRESS: 진행중, COMPLETED: 완료
        BookmarkMissionStatus status,
        // COMPLETED면 완료 날짜, 그 외에는 저장 날짜 (M.dd, KST)
        String date
) {
    public static BookmarkItemResponse of(MissionBookmark bookmark, BookmarkMissionStatus status, LocalDate date) {
        Mission mission = bookmark.getMission();
        return new BookmarkItemResponse(
                bookmark.getId(),
                mission.getId(),
                mission.getTitle(),
                status,
                BookmarkDateFormatter.format(date)
        );
    }
}
