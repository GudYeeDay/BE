package org.example.gudyeeday.domain.notification.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationType {

    // 매일 9시 전체 사용자에게 보내는 오늘의 굳이 (조건 없음)
    DAILY_MISSION(
            "오늘의 굳이", "오늘은 평소와 조금 다른 일을 해보는 건 어때요?",
            "오늘의 굳이", "오늘은 평소와 조금 다른 일을 해보는 건 어때요?"),

    // 진행중인 미션을 아직 완료하지 않은 사용자에게 매일 20시 보내는 리마인드 (알림함 내용: 미션 이름)
    MISSION_REMINDER(
            "아직 오늘의 굳이를 안 남기셨네요", "%s - 잊지 않으셨죠?",
            "미션 리마인드", "오늘 해본 경험을 사진과 글로 남겨보세요."),

    // 관리자가 전체 사용자에게 보내는 서비스 업데이트 소식 (알림함 제목/내용: 관리자가 입력)
    SERVICE_UPDATE(
            "서비스 업데이트", "%s",
            "서비스 업데이트", "굳이가 더 편리해졌어요. 새로운 소식을 확인해보세요.");

    // 알림함에 보이는 제목/내용 형식
    private final String title;
    private final String contentFormat;

    // 폰 푸시로 보이는 제목/내용
    private final String pushTitle;
    private final String pushBody;

    public String formatContent(Object... args) {
        return String.format(contentFormat, args);
    }
}
