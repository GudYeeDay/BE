package org.example.gudyeeday.domain.mission.dto.response;

import java.util.List;

public record BookmarkListResponse(
        // 보관함 전체 개수 (탭과 무관)
        int totalCount,
        // 선택한 탭에 해당하는 미션 (최근 저장순)
        List<BookmarkItemResponse> bookmarks
) {
}
