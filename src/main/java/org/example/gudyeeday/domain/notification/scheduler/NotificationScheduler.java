package org.example.gudyeeday.domain.notification.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.gudyeeday.domain.notification.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationService notificationService;

    // 발송 시각은 notification.daily-mission.cron (KST, 기본 매일 9시)
    @Scheduled(cron = "${notification.daily-mission.cron}", zone = "Asia/Seoul")
    public void sendDailyMission() {
        notificationService.sendDailyMission();
    }

    // 발송 시각은 notification.mission-reminder.cron (KST)
    @Scheduled(cron = "${notification.mission-reminder.cron}", zone = "Asia/Seoul")
    public void sendMissionReminders() {
        notificationService.sendMissionReminders();
    }
}
