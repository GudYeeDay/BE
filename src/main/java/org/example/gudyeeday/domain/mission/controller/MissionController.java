package org.example.gudyeeday.domain.mission.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.gudyeeday.common.response.ApiResponse;
import org.example.gudyeeday.domain.mission.dto.response.InProgressMissionResponse;
import org.example.gudyeeday.domain.mission.dto.response.MissionBookmarkResponse;
import org.example.gudyeeday.domain.mission.dto.response.MissionRecommendResponse;
import org.example.gudyeeday.domain.mission.service.MissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Mission API", description = "미션 - 추천/진행중 미션")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionService missionService;

    @Operation(
            summary = "미션 추천",
            description = "오늘(KST) 요일(평일/주말)과 계절에 맞는 미션 중 랜덤으로 최대 3개를 반환합니다. 보관함에 있는 미션, 진행중이거나 완료한 미션, 나만의 굳이 미션은 제외됩니다. 다시 호출하면 새로 추첨합니다.")
    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<List<MissionRecommendResponse>>> recommendMissions(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.onSuccess(missionService.recommendMissions(userDetails.getUsername())));
    }

    @Operation(
            summary = "미션 보관함 저장(북마크)",
            description = "보관함에서 삭제했던 미션이면 다시 저장됩니다(저장 날짜 갱신). 이미 저장된 미션이면 409(MISSION4092)를 반환합니다.")
    @PostMapping("/{missionId}/bookmarks")
    public ResponseEntity<ApiResponse<MissionBookmarkResponse>> bookmarkMission(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long missionId) {
        return ResponseEntity.ok(ApiResponse.onSuccess(missionService.bookmarkMission(userDetails.getUsername(), missionId)));
    }

    @Operation(
            summary = "미션 보관함 저장 해제(북마크 해제)",
            description = "보관함의 모든 탭에서 사라집니다(소프트 삭제). 보관함에 저장되지 않은 미션이면 404(MISSION4043)를 반환합니다.")
    @DeleteMapping("/{missionId}/bookmarks")
    public ResponseEntity<ApiResponse<Void>> unbookmarkMission(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long missionId) {
        missionService.unbookmarkMission(userDetails.getUsername(), missionId);
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }

    @Operation(
            summary = "미션 시작",
            description = "미션을 진행중 상태로 저장합니다. 이미 진행중인 미션이 있으면 409(MISSION4091), 존재하지 않는 미션이면 404(MISSION4041)를 반환합니다.")
    @PostMapping("/{missionId}/start")
    public ResponseEntity<ApiResponse<InProgressMissionResponse>> startMission(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long missionId) {
        return ResponseEntity.ok(ApiResponse.onSuccess(missionService.startMission(userDetails.getUsername(), missionId)));
    }

    @Operation(
            summary = "진행중인 미션 조회",
            description = "진행중인 미션이 없으면 404(MISSION4042)를 반환합니다.")
    @GetMapping("/in-progress")
    public ResponseEntity<ApiResponse<InProgressMissionResponse>> getInProgressMission(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.onSuccess(missionService.getInProgressMission(userDetails.getUsername())));
    }

    @Operation(
            summary = "진행중인 미션 완료",
            description = "진행중인 미션을 완료 상태로 바꿉니다. 보관함에 없던 미션은 보관함에 저장되어 완료한 미션 목록에 나옵니다(보관함에서 삭제한 미션은 제외). 완료 후에는 새 미션을 시작할 수 있습니다. 진행중인 미션이 없으면 404(MISSION4042)를 반환합니다.")
    @PostMapping("/in-progress/complete")
    public ResponseEntity<ApiResponse<InProgressMissionResponse>> completeInProgressMission(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.onSuccess(missionService.completeInProgressMission(userDetails.getUsername())));
    }

    @Operation(
            summary = "진행중인 미션 그만두기",
            description = "진행 기록을 삭제합니다. 진행중인 미션이 없으면 404(MISSION4042)를 반환합니다.")
    @DeleteMapping("/in-progress")
    public ResponseEntity<ApiResponse<Void>> quitInProgressMission(@AuthenticationPrincipal UserDetails userDetails) {
        missionService.quitInProgressMission(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }
}
