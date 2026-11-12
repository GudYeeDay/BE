package org.example.gudyeeday.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gudyeeday.common.exception.CustomException;
import org.example.gudyeeday.domain.mission.entity.UserMission;
import org.example.gudyeeday.domain.mission.enums.UserMissionStatus;
import org.example.gudyeeday.domain.mission.repository.UserMissionRepository;
import org.example.gudyeeday.domain.notification.dto.response.NotificationResponse;
import org.example.gudyeeday.domain.notification.entity.Notification;
import org.example.gudyeeday.domain.notification.enums.NotificationType;
import org.example.gudyeeday.domain.notification.exception.NotificationErrorCode;
import org.example.gudyeeday.domain.notification.push.NotificationPushEvent;
import org.example.gudyeeday.domain.notification.push.PushRequest;
import org.example.gudyeeday.domain.notification.repository.NotificationRepository;
import org.example.gudyeeday.domain.user.entity.User;
import org.example.gudyeeday.domain.user.exception.AuthErrorCode;
import org.example.gudyeeday.domain.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserMissionRepository userMissionRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    // 알림함 목록 (최신순)
    public List<NotificationResponse> getNotifications(String email) {
        User user = getUser(email);
        return notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(user.getId()).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    // 알림 상세 (조회하면 읽음 처리)
    @Transactional
    public NotificationResponse getNotification(String email, Long notificationId) {
        User user = getUser(email);
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, user.getId())
                .orElseThrow(() -> new CustomException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        notification.read(LocalDateTime.now(clock));
        return NotificationResponse.from(notification);
    }

    // 전체 사용자에게 오늘의 굳이 알림 생성 + 폰 푸시 (오늘 이미 받은 사용자는 제외)
    @Transactional
    public int sendDailyMission() {
        NotificationType type = NotificationType.DAILY_MISSION;
        LocalDateTime todayStart = LocalDate.now(clock).atStartOfDay();
        Set<Long> alreadyNotified = new HashSet<>(
                notificationRepository.findUserIdsByTypeAndCreatedAtSince(type, todayStart)
        );

        List<Notification> notifications = userRepository.findAll().stream()
                .filter(user -> !alreadyNotified.contains(user.getId()))
                .map(user -> Notification.createNotification(user, type, type.formatContent()))
                .toList();

        notificationRepository.saveAll(notifications);
        publishPush(notifications);
        log.info("오늘의 굳이 알림 {}건 생성", notifications.size());
        return notifications.size();
    }

    // 진행중인 미션이 있는 사용자에게 리마인드 알림 생성 (오늘 이미 받은 사용자는 제외)
    @Transactional
    public int sendMissionReminders() {
        LocalDateTime todayStart = LocalDate.now(clock).atStartOfDay();
        Set<Long> alreadyNotified = new HashSet<>(
                notificationRepository.findUserIdsByTypeAndCreatedAtSince(NotificationType.MISSION_REMINDER, todayStart)
        );

        List<Notification> notifications = userMissionRepository.findAllByStatus(UserMissionStatus.IN_PROGRESS).stream()
                .filter(userMission -> !alreadyNotified.contains(userMission.getUser().getId()))
                .map(this::missionReminder)
                .toList();

        notificationRepository.saveAll(notifications);
        publishPush(notifications);
        log.info("미션 리마인드 알림 {}건 생성", notifications.size());
        return notifications.size();
    }

    private Notification missionReminder(UserMission userMission) {
        NotificationType type = NotificationType.MISSION_REMINDER;
        return Notification.createNotification(
                userMission.getUser(),
                type,
                type.formatContent(userMission.getMission().getTitle())
        );
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
    }

    // 전체 사용자에게 서비스 업데이트 알림 생성 + 폰 푸시
    @Transactional
    public int sendServiceUpdate(String title, String content) {
        NotificationType type = NotificationType.SERVICE_UPDATE;
        String notificationTitle = StringUtils.hasText(title) ? title.trim() : type.getTitle();

        List<Notification> notifications = userRepository.findAll().stream()
                .map(user -> Notification.createNotification(user, type, notificationTitle, content.trim()))
                .toList();

        notificationRepository.saveAll(notifications);
        publishPush(notifications);
        log.info("서비스 업데이트 알림 {}건 생성", notifications.size());
        return notifications.size();
    }

    // 푸시는 알림함 저장이 커밋된 뒤 NotificationPushListener에서 발송
    private void publishPush(List<Notification> notifications) {
        if (notifications.isEmpty()) {
            return;
        }
        List<PushRequest> requests = notifications.stream()
                .map(PushRequest::from)
                .toList();
        eventPublisher.publishEvent(new NotificationPushEvent(requests));
    }
}
