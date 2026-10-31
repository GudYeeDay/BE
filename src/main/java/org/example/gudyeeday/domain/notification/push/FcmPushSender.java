package org.example.gudyeeday.domain.notification.push;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "true")
public class FcmPushSender implements PushSender {

    // 토큰을 더 쓸 수 없다는 FCM 에러 (앱 삭제/토큰 만료, 다른 Firebase 프로젝트의 토큰)
    private static final Set<MessagingErrorCode> INVALID_TOKEN_ERRORS =
            EnumSet.of(MessagingErrorCode.UNREGISTERED, MessagingErrorCode.SENDER_ID_MISMATCH);

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public PushSendResult send(List<PushMessage> messages) {
        BatchResponse batchResponse;
        try {
            batchResponse = firebaseMessaging.sendEach(messages.stream().map(this::toMessage).toList());
        } catch (FirebaseMessagingException e) {
            throw new IllegalStateException("FCM 발송 요청 실패", e);
        }

        // 응답 순서는 보낸 메시지 순서와 같음
        List<String> invalidTokens = new ArrayList<>();
        List<SendResponse> responses = batchResponse.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse response = responses.get(i);
            if (response.isSuccessful()) {
                continue;
            }
            FirebaseMessagingException exception = response.getException();
            if (exception != null && INVALID_TOKEN_ERRORS.contains(exception.getMessagingErrorCode())) {
                invalidTokens.add(messages.get(i).token());
            } else {
                log.warn("FCM 발송 실패: {}", exception != null ? exception.getMessage() : "알 수 없는 오류");
            }
        }

        return new PushSendResult(batchResponse.getSuccessCount(), batchResponse.getFailureCount(), invalidTokens);
    }

    private Message toMessage(PushMessage push) {
        return Message.builder()
                .setToken(push.token())
                .setNotification(Notification.builder()
                        .setTitle(push.title())
                        .setBody(push.body())
                        .build())
                .putAllData(push.data())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder().setSound("default").build())
                        .build())
                .build();
    }
}