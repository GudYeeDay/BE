package org.example.gudyeeday.domain.notification.push;

import java.util.Map;

// 기기 하나로 보낼 푸시 한 건
public record PushMessage(
        String token,
        String title,
        String body,
        // 앱에서 알림을 눌렀을 때 쓸 값 (type, notificationId)
        Map<String, String> data
) {
}