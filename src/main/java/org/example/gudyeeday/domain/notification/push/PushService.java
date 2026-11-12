package org.example.gudyeeday.domain.notification.push;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gudyeeday.domain.notification.repository.DeviceTokenRepository;
import org.example.gudyeeday.domain.notification.service.DeviceTokenService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 사용자별 푸시 요청을 기기 토큰으로 펼쳐 발송
// 푸시가 실패해도 알림함 알림은 그대로 남아야 하므로 예외를 밖으로 던지지 않음
@Slf4j
@Service
@RequiredArgsConstructor
public class PushService {

    // FCM sendEach 한 번에 보낼 수 있는 최대 건수
    static final int SEND_BATCH_SIZE = 500;
    // 토큰 조회 IN 절 크기
    static final int USER_ID_CHUNK_SIZE = 1000;

    private final DeviceTokenRepository deviceTokenRepository;
    private final DeviceTokenService deviceTokenService;
    private final PushSender pushSender;

    public void send(List<PushRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }

        try {
            List<PushMessage> messages = toMessages(requests);
            if (messages.isEmpty()) {
                log.info("푸시 대상 기기 없음 (사용자 {}명)", requests.size());
                return;
            }

            int success = 0;
            int failure = 0;
            List<String> invalidTokens = new ArrayList<>();

            for (List<PushMessage> batch : chunk(messages, SEND_BATCH_SIZE)) {
                try {
                    PushSendResult result = pushSender.send(batch);
                    success += result.successCount();
                    failure += result.failureCount();
                    invalidTokens.addAll(result.invalidTokens());
                } catch (Exception e) {
                    failure += batch.size();
                    log.error("푸시 발송 실패 ({}건)", batch.size(), e);
                }
            }

            if (!invalidTokens.isEmpty()) {
                deviceTokenService.deleteTokens(invalidTokens);
            }
            log.info("푸시 발송 완료: 기기 {}대 중 성공 {}, 실패 {}, 만료 토큰 삭제 {}",
                    messages.size(), success, failure, invalidTokens.size());
        } catch (Exception e) {
            log.error("푸시 발송 처리 중 오류", e);
        }
    }

    private List<PushMessage> toMessages(List<PushRequest> requests) {
        // 같은 사용자에게 요청이 여러 개면 마지막 것만 보냄
        Map<Long, PushRequest> requestByUserId = new LinkedHashMap<>();
        requests.forEach(request -> requestByUserId.put(request.userId(), request));

        List<PushMessage> messages = new ArrayList<>();
        for (List<Long> userIds : chunk(new ArrayList<>(requestByUserId.keySet()), USER_ID_CHUNK_SIZE)) {
            for (PushTarget target : deviceTokenRepository.findPushTargetsByUserIdIn(userIds)) {
                PushRequest request = requestByUserId.get(target.userId());
                messages.add(new PushMessage(target.token(), request.title(), request.body(), request.data()));
            }
        }
        return messages;
    }

    private static <T> List<List<T>> chunk(List<T> items, int size) {
        List<List<T>> chunks = new ArrayList<>();
        for (int from = 0; from < items.size(); from += size) {
            chunks.add(items.subList(from, Math.min(from + size, items.size())));
        }
        return chunks;
    }
}