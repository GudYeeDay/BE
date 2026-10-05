package org.example.gudyeeday.domain.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.gudyeeday.common.response.ApiResponse;
import org.example.gudyeeday.domain.notification.dto.response.NotificationResponse;
import org.example.gudyeeday.domain.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Notification API", description = "알림함")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "알림 목록",
            description = """
                    받은 알림을 최신순으로 반환합니다.
                    - type: MISSION_REMINDER(진행중인 미션 리마인드, 매일 정해진 시각에 진행중인 미션이 있는 사용자에게 생성)
                    - read: 읽음 여부 (상세 조회 시 읽음 처리)
                    - createdAt: 알림 발송 시각 (KST, yyyy-MM-dd'T'HH:mm:ss)""")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.onSuccess(notificationService.getNotifications(userDetails.getUsername())));
    }

    @Operation(
            summary = "알림 상세",
            description = "알림을 읽음 처리하고 반환합니다. 본인 알림이 아니거나 없는 알림이면 404(NOTIFICATION4041)를 반환합니다.")
    @GetMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotification(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long notificationId) {
        return ResponseEntity.ok(ApiResponse.onSuccess(notificationService.getNotification(userDetails.getUsername(), notificationId)));
    }
}
