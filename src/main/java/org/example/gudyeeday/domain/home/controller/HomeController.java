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
                    - monthlyCompletedDays: 이번 달(KST) 미션을 완료한 날의 수. 하루에 여러 미션을 완료해도 1일로 셉니다.
                    - weeklyPhotos: 오늘(KST)이 속한 주(월요일~일요일)의 사진. startDate/endDate는 이번 주 월요일/일요일(yyyy-MM-dd)이고, days는 월요일부터 7일입니다.
                      today는 오늘 여부이며, 그날 올린 사진이 없으면 weeklyPhotoId/imageUrl이 null입니다. 주가 바뀌면 지난 주 사진은 포함되지 않습니다.
                    - pastRecords: 이번 주 월요일 0시(KST) 이전에 완료한 미션 기록 중 랜덤 최대 5개. 호출할 때마다 새로 추첨합니다. 기록이 없으면 빈 배열입니다.
                      title/description은 미션 이름/설명, date는 미션 완료 날짜(yyyy-MM-dd), location은 기록한 위치(없으면 null)입니다.""")
    @GetMapping
    public ResponseEntity<ApiResponse<HomeResponse>> getHome(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.onSuccess(homeService.getHome(userDetails.getUsername())));
    }

    @Operation(
            summary = "오늘 사진 업로드",
            description = "오늘(KST) 날짜로 사진을 저장합니다. 하루 1장이며, 이미 올린 사진이 있으면 새 사진으로 교체됩니다. 파일이 없으면 400(HOME4001), 이미지가 아니면 400(HOME4002)을 반환합니다.")
    @PostMapping(value = "/weekly-photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<WeeklyPhotoResponse>> uploadTodayPhoto(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.onSuccess(homeService.uploadTodayPhoto(userDetails.getUsername(), file)));
    }
}
