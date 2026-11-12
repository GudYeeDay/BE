package org.example.gudyeeday.domain.notification.push;

import org.example.gudyeeday.domain.notification.entity.Notification;
import org.example.gudyeeday.domain.notification.enums.NotificationType;

import java.util.HashMap;
import java.util.Map;

// 사용자 한 명에게 보낼 푸시 (기기가 여러 대면 모든 기기로 감)
public record PushRequest(Long userId, String title, String body, Map<String, String> data) {

    // 알림함에 저장한 알림을 푸시 문구로 변환 (푸시 문구는 NotificationType 기준)
    public static PushRequest from(Notification notification) {
        NotificationType type = notification.getType();

        Map<String, String> data = new HashMap<>();
        data.put("type", type.name());
        if (notification.getId() != null) {
            data.put("notificationId", String.valueOf(notification.getId()));
        }

        return new PushRequest(
                notification.getUser().getId(),
                type.getPushTitle(),
                type.getPushBody(),
                Map.copyOf(data)
        );
    }
}