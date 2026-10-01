package org.example.gudyeeday.domain.mission.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gudyeeday.common.response.ApiResponse;
import org.example.gudyeeday.domain.mission.dto.request.CustomMissionCreateRequest;
import org.example.gudyeeday.domain.mission.dto.response.BookmarkDetailResponse;
import org.example.gudyeeday.domain.mission.dto.response.BookmarkListResponse;
import org.example.gudyeeday.domain.mission.dto.response.InProgressMissionResponse;
import org.example.gudyeeday.domain.mission.enums.BookmarkFilter;
import org.example.gudyeeday.domain.mission.service.BookmarkService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Bookmark API", description = "보관함 - 저장한 미션/나만의 굳이 미션")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @Operation(
            summary = "보관함 목록",
            description = """
                    보관함 목록을 최근 저장순으로 반환합니다. 완료한 미션은 저장하지 않았어도 완료 시 보관함에 들어갑니다.
                    - filter: ALL(전체, 기본값) / REMAINING(남은 미션: 저장했고 아직 완료하지 않은 미션, 진행중 포함) / COMPLETED(완료한 미션)
                    - totalCount: filter와 무관한 보관함 전체 개수
                    - status: SAVED(저장만 함) / IN_PROGRESS(진행중) / COMPLETED(완료)
                    - date: COMPLETED면 완료 날짜, 그 외에는 저장 날짜 (M.dd, KST)""")
    @GetMapping
    public ResponseEntity<ApiResponse<BookmarkListResponse>> getBookmarks(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "ALL / REMAINING / COMPLETED") @RequestParam(defaultValue = "ALL") BookmarkFilter filter) {
        return ResponseEntity.ok(ApiResponse.onSuccess(bookmarkService.getBookmarks(userDetails.getUsername(), filter)));
    }

    @Operation(
            summary = "저장한 미션 상세",
            description = "저장 날짜(M.dd, KST), 미션 제목, 설명을 반환합니다. 본인 보관함에 없는 항목이면 404(MISSION4043)를 반환합니다.")
    @GetMapping("/{bookmarkId}")
    public ResponseEntity<ApiResponse<BookmarkDetailResponse>> getBookmark(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long bookmarkId) {
        return ResponseEntity.ok(ApiResponse.onSuccess(bookmarkService.getBookmark(userDetails.getUsername(), bookmarkId)));
    }

    @Operation(
            summary = "나만의 굳이 미션 추가",
            description = "미션 이름(최대 100자)과 간단한 설명(최대 500자)으로 미션을 만들고 보관함에 저장합니다. 나만의 굳이 미션은 추천에 나오지 않습니다.")
    @PostMapping("/custom")
    public ResponseEntity<ApiResponse<BookmarkDetailResponse>> createCustomMission(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CustomMissionCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.onSuccess(bookmarkService.createCustomMission(userDetails.getUsername(), request)));
    }

    @Operation(
            summary = "보관함에서 삭제",
            description = "보관함의 모든 탭에서 사라집니다(소프트 삭제). 진행중/완료 기록은 유지됩니다. 본인 보관함에 없는 항목이면 404(MISSION4043)를 반환합니다.")
    @DeleteMapping("/{bookmarkId}")
    public ResponseEntity<ApiResponse<Void>> deleteBookmark(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long bookmarkId) {
        bookmarkService.deleteBookmark(userDetails.getUsername(), bookmarkId);
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }

    @Operation(
            summary = "이 미션으로 시작하기",
            description = "저장한 미션을 진행중 상태로 시작합니다. 완료한 미션도 다시 시작할 수 있습니다. 이미 진행중인 미션이 있으면 409(MISSION4091), 본인 보관함에 없는 항목이면 404(MISSION4043)를 반환합니다.")
    @PostMapping("/{bookmarkId}/start")
    public ResponseEntity<ApiResponse<InProgressMissionResponse>> startBookmarkedMission(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long bookmarkId) {
        return ResponseEntity.ok(ApiResponse.onSuccess(bookmarkService.startBookmarkedMission(userDetails.getUsername(), bookmarkId)));
    }
}
