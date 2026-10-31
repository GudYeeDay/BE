package org.example.gudyeeday.domain.notification.push;

import java.util.List;

public record PushSendResult(
        int successCount,
        int failureCount,
        // 앱 삭제 등으로 더 이상 쓸 수 없는 토큰 (DB에서 지움)
        List<String> invalidTokens
) {
}