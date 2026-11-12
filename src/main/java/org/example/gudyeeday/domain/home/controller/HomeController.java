package org.example.gudyeeday.domain.home.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.gudyeeday.common.response.ApiResponse;
import org.example.gudyeeday.domain.home.dto.response.HomeResponse;
import org.example.gudyeeday.domain.home.dto.response.WeeklyPhotoResponse;
import org.example.gudyeeday.domain.home.service.HomeService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Home API", description = "홈 - 주차별 낭만 기록 사진/이번 달 기록 일수/지난 낭만들")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home")
public class HomeController {

    private final HomeService homeService;

    @Operation(
            summary = "홈 조회",
            description = """
                    - name: 사용자 닉네임
                    - hasUnreadNotification: 안 읽은 알림이 있는지
                    - monthlyCompletedDays: 이번 달(KST) 기록을 남긴 날의 수. 하루에 여러 기록을 남겨도 1일로 세며, 기록을 삭제해 그날 기록이 없어지면 빠집니다.
                    - weeklyPhotos: 오늘(KST)이 속한 주(월요일~일요일)의 기록 사진. startDate/endDate는 이번 주 월요일/일요일(yyyy-MM-dd)이고, days는 월요일부터 7일입니다.
                      각 날짜의 사진은 그날 남긴 기록(POST /api/complete/{userMissionId})의 사진이며, 하루에 기록이 여러 개면 가장 늦게 완료한 기록의 사진입니다.
                      completeMissionId는 그 기록의 id이고, 그날 기록이 없으면 completeMissionId/imageUrl이 null입니다. 기록을 수정·삭제하면 바로 반영됩니다.
                      today는 오늘 여부입니다. 주가 바뀌면 지난 주는 포함되지 않습니다.
                    - pastRecords: 이번 주 월요일 0시(KST) 이전에 완료한 미션 기록 중 랜덤 최대 5개. 호출할 때마다 새로 추첨합니다. 기록이 없으면 빈 배열입니다.
                      title/description은 미션 이름/설명, date는 미션 완료 날짜(yyyy-MM-dd), location은 기록한 위치(없으면 null)입니다.""")
    @GetMapping
    public ResponseEntity<ApiResponse<HomeResponse>> getHome(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.onSuccess(homeService.getHome(userDetails.getUsername())));
    }

    @Deprecated
    @Operation(
            summary = "오늘 사진 업로드 (사용 중단)",
            deprecated = true,
            description = "사용 중단 예정입니다. 홈 주간 사진은 이제 기록 사진을 자동으로 보여 주므로, 여기서 올린 사진은 홈에 나오지 않습니다. 호출을 빼 주세요. 파일이 없으면 400(HOME4001), 이미지가 아니면 400(HOME4002)을 반환합니다.")
    @PostMapping(value = "/weekly-photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<WeeklyPhotoResponse>> uploadTodayPhoto(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.onSuccess(homeService.uploadTodayPhoto(userDetails.getUsername(), file)));
    }
}
