package org.example.gudyeeday.domain.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gudyeeday.common.admin.AdminKeyValidator;
import org.example.gudyeeday.common.response.ApiResponse;
import org.example.gudyeeday.domain.notification.dto.request.ServiceUpdateNotificationRequest;
import org.example.gudyeeday.domain.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Notification API", description = "관리자 - 알림 발송 (X-Admin-Key 헤더 필요)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {

    private static final String ADMIN_KEY_HEADER = "X-Admin-Key";

    private final NotificationService notificationService;
    private final AdminKeyValidator adminKeyValidator;

    @Operation(
            summary = "서비스 업데이트 알림 발송",
            description = """
                    전체 사용자의 알림함에 서비스 업데이트 알림을 만들고 폰 푸시를 보냅니다.
                    - 푸시 문구: '서비스 업데이트' / '굳이가 더 편리해졌어요. 새로운 소식을 확인해보세요.'
                    - 알림함 제목/본문: 요청의 title(비우면 '서비스 업데이트')/content
                    - 결과: 알림을 만든 사용자 수
                    X-Admin-Key가 틀리거나 서버에 ADMIN_API_KEY가 없으면 403(COMMON403_1)을 반환합니다.""")
    @PostMapping("/service-update")
    public ResponseEntity<ApiResponse<Integer>> sendServiceUpdate(
            @Parameter(description = "서버 환경변수 ADMIN_API_KEY 값") @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String adminKey,
            @Valid @RequestBody ServiceUpdateNotificationRequest request) {
        adminKeyValidator.validate(adminKey);
        return ResponseEntity.ok(ApiResponse.onSuccess(
                notificationService.sendServiceUpdate(request.title(), request.content())));
    }

    @Operation(
            summary = "미션 리마인드 즉시 발송 (테스트용)",
            description = """
                    매일 20시 스케줄과 같은 리마인드를 지금 바로 보냅니다. 오늘 이미 받은 사용자는 제외됩니다.
                    결과: 알림을 만든 사용자 수""")
    @PostMapping("/mission-reminder")
    public ResponseEntity<ApiResponse<Integer>> sendMissionReminders(
            @Parameter(description = "서버 환경변수 ADMIN_API_KEY 값") @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String adminKey) {
        adminKeyValidator.validate(adminKey);
        return ResponseEntity.ok(ApiResponse.onSuccess(notificationService.sendMissionReminders()));
    }
}