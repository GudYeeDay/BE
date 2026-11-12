package org.example.gudyeeday.domain.notification.push;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

// FCM 설정이 없을 때(로컬/테스트) 폰으로 보내지 않고 로그만 남김
@Slf4j
@Component
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingPushSender implements PushSender {

    @Override
    public PushSendResult send(List<PushMessage> messages) {
        messages.forEach(message -> log.info("[푸시 미발송 - fcm.enabled=false] title={}, body={}, data={}",
                message.title(), message.body(), message.data()));
        return new PushSendResult(0, 0, List.of());
    }
}