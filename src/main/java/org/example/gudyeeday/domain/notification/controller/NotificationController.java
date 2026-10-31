package org.example.gudyeeday.domain.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gudyeeday.common.response.ApiResponse;
import org.example.gudyeeday.domain.notification.dto.request.DeviceTokenRequest;
import org.example.gudyeeday.domain.notification.dto.response.NotificationResponse;
import org.example.gudyeeday.domain.notification.service.DeviceTokenService;
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
    private final DeviceTokenService deviceTokenService;

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

    @Operation(
            summary = "푸시 기기 토큰 등록",
            description = """
                    앱에서 Firebase로 받은 FCM 토큰을 등록합니다. 로그인 직후와 토큰이 바뀔 때(onTokenRefresh) 호출해 주세요.
                    같은 토큰이 다른 계정에 등록돼 있으면 지금 로그인한 계정으로 옮겨집니다.""")
    @PostMapping("/device-token")
    public ResponseEntity<ApiResponse<Void>> registerDeviceToken(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DeviceTokenRequest request) {
        deviceTokenService.registerToken(userDetails.getUsername(), request.token());
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }

    @Operation(
            summary = "푸시 기기 토큰 해제",
            description = "로그아웃하기 전에 호출하면 이 기기로 더 이상 푸시가 가지 않습니다. 등록되지 않은 토큰이어도 성공합니다.")
    @DeleteMapping("/device-token")
    public ResponseEntity<ApiResponse<Void>> deleteDeviceToken(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DeviceTokenRequest request) {
        deviceTokenService.deleteToken(userDetails.getUsername(), request.token());
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }
}
