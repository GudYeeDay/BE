package org.example.gudyeeday.domain.notification.push;

import java.util.List;

// 알림함 저장 후 푸시를 보내기 위한 이벤트
public record NotificationPushEvent(List<PushRequest> requests) {
}