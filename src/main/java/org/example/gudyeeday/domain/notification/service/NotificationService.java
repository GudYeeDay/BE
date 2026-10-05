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
import org.example.gudyeeday.domain.notification.repository.NotificationRepository;
import org.example.gudyeeday.domain.user.entity.User;
import org.example.gudyeeday.domain.user.exception.AuthErrorCode;
import org.example.gudyeeday.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
