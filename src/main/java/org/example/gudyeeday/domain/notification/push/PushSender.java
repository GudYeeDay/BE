package org.example.gudyeeday.domain.notification.push;

import java.util.List;

// 실제 발송 방식 (fcm.enabled=true: FCM, false: 로그만)
public interface PushSender {

    // 한 번에 최대 500건
    PushSendResult send(List<PushMessage> messages);
}