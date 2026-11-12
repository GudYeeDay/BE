package org.example.gudyeeday.domain.complete.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gudyeeday.common.response.ApiResponse;
import org.example.gudyeeday.domain.complete.dto.request.CompleteMissionRequest;
import org.example.gudyeeday.domain.complete.dto.response.CompleteMissionResponse;
import org.example.gudyeeday.domain.complete.service.CompleteService;
import org.example.gudyeeday.domain.mypage.dto.request.NameChangeRequest;
import org.example.gudyeeday.domain.mypage.dto.request.PasswordChangeRequest;
import org.example.gudyeeday.domain.mypage.dto.response.ProfileResponse;
import org.example.gudyeeday.domain.mypage.service.MypageService;
import org.example.gudyeeday.domain.user.service.EmailVerificationService;
import org.example.gudyeeday.domain.user.service.UserService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Tag(name = "Complete API", description = "미션 완료")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/complete")
public class CompleteController {

    private final CompleteService completeService;

    @Operation(
            summary = "미션 완료하기",
            description = """
                    진행한 미션을 사진(image, 필수)과 위치(location, 최대 200자)·내용(content, 최대 500자)으로 기록하고 완료 상태로 바꿉니다.
                    보관함에 없던 미션은 보관함에 저장되어 완료한 미션 목록에 나옵니다. 기록 사진은 홈 주간 사진에도 쓰입니다.
                    빈 사진이면 400(COMPLETE6003), 내 미션이 아니면 403(COMPLETE6002), 이미 기록한 미션이면 409(COMPLETE6001), 존재하지 않는 미션이면 404(MISSION4041)를 반환합니다.""")
    @PostMapping(value = "/{userMissionId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CompleteMissionResponse>> completeMission(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userMissionId,
            @RequestPart("image") MultipartFile image,
            @Valid @ParameterObject @ModelAttribute CompleteMissionRequest request) {

        CompleteMissionResponse response = completeService.completeMission(userDetails.getUsername(), userMissionId, image, request);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(summary = "데일리피드", description = "date 파라미터(yyyy-MM-dd)가 있으면 해당 날짜, 없으면 오늘의 피드를 조회합니다.")
    @GetMapping(value = "/dailyFeed")
    public ResponseEntity<ApiResponse<List<CompleteMissionResponse>>> dailyFeed(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "조회할 날짜 (예: 2026-10-29), 미입력 시 오늘")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = (date != null) ? date : LocalDate.now(ZoneId.of("Asia/Seoul"));
        List<CompleteMissionResponse> response = completeService.dailyFeed(userDetails.getUsername(), targetDate);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(summary = "기록 수정", description = "")
    @PutMapping(value = "/{completeMissionId}/update",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CompleteMissionResponse>> updateRecord(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long completeMissionId,
            @RequestPart(name = "image", required = false) MultipartFile image,
            @Valid @ParameterObject @ModelAttribute CompleteMissionRequest request
            ){
        CompleteMissionResponse response = completeService.updateRecord(userDetails.getUsername(), completeMissionId, image, request);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(summary = "기록 삭제", description = "")
    @DeleteMapping(value = "/{completeMissionId}/delete")
    public ResponseEntity<ApiResponse<Long>> deleteRecord(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long completeMissionId
    ){
        Long deleteCompleteId = completeService.deleteRecord(userDetails.getUsername(), completeMissionId);

        return ResponseEntity.ok(ApiResponse.onSuccess(deleteCompleteId));
    }

}
