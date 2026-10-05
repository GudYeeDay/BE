package org.example.gudyeeday.domain.notification.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationType {

    // 진행중인 미션을 아직 완료하지 않은 사용자에게 보내는 리마인드 (내용: 미션 이름)
    MISSION_REMINDER("아직 오늘의 굳이를 안 남기셨네요", "%s - 잊지 않으셨죠?");

    private final String title;
    private final String contentFormat;

    public String formatContent(Object... args) {
        return String.format(contentFormat, args);
    }
}
