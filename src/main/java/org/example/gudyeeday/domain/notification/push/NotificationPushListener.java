package org.example.gudyeeday.domain.notification.push;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationPushListener {

    private final PushService pushService;

    // 알림함 저장이 커밋된 뒤에만 푸시 (저장이 롤백되면 푸시도 안 감)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(NotificationPushEvent event) {
        pushService.send(event.requests());
    }
}