package org.example.gudyeeday.domain.notification.service;

import org.example.gudyeeday.common.exception.CustomException;
import org.example.gudyeeday.domain.mission.entity.Mission;
import org.example.gudyeeday.domain.mission.entity.UserMission;
import org.example.gudyeeday.domain.mission.enums.DayType;
import org.example.gudyeeday.domain.mission.enums.Season;
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
import org.example.gudyeeday.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private static final String EMAIL = "user@test.com";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    // 2026-10-05(월) 20:00 KST
    private static final ZonedDateTime NOW = ZonedDateTime.of(2026, 10, 5, 20, 0, 0, 0, KST);

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final UserMissionRepository userMissionRepository = mock(UserMissionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final Clock clock = Clock.fixed(NOW.toInstant(), KST);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private NotificationService notificationService;
    private User user;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, userMissionRepository, userRepository, clock, eventPublisher);
        user = user(1L, EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    @Test
    void 오늘의_굳이는_오늘_아직_받지_않은_전체_사용자에게_만들고_푸시_이벤트를_발행한다() {
        User notified = user(2L, "notified@test.com");
        User other = user(3L, "other@test.com");
        when(userRepository.findAll()).thenReturn(List.of(user, notified, other));
        when(notificationRepository.findUserIdsByTypeAndCreatedAtSince(NotificationType.DAILY_MISSION, LocalDateTime.of(2026, 10, 5, 0, 0)))
                .thenReturn(List.of(2L));

        int count = notificationService.sendDailyMission();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> notificationCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(notificationCaptor.capture());
        assertThat(count).isEqualTo(2);
        assertThat(notificationCaptor.getValue())
                .extracting(Notification::getUser)
                .containsExactly(user, other);
        assertThat(notificationCaptor.getValue()).allSatisfy(notification -> {
            assertThat(notification.getType()).isEqualTo(NotificationType.DAILY_MISSION);
            assertThat(notification.getTitle()).isEqualTo("오늘의 굳이");
            assertThat(notification.getContent()).isEqualTo("오늘은 평소와 조금 다른 일을 해보는 건 어때요?");
        });

        ArgumentCaptor<NotificationPushEvent> eventCaptor = ArgumentCaptor.forClass(NotificationPushEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().requests())
                .extracting(PushRequest::userId, PushRequest::title, PushRequest::body)
                .containsExactly(
                        tuple(1L, "오늘의 굳이", "오늘은 평소와 조금 다른 일을 해보는 건 어때요?"),
                        tuple(3L, "오늘의 굳이", "오늘은 평소와 조금 다른 일을 해보는 건 어때요?")
                );
    }

    @Test
    void 리마인드는_진행중인_미션이_있고_오늘_아직_받지_않은_사용자에게_미션_이름으로_만든다() {
        User notified = user(2L, "notified@test.com");
        when(userMissionRepository.findAllByStatus(UserMissionStatus.IN_PROGRESS)).thenReturn(List.of(
                inProgress(user, "혼자 가는 카페에서 창가 자리 앉기"),
                inProgress(notified, "다른 미션")
        ));
        when(notificationRepository.findUserIdsByTypeAndCreatedAtSince(NotificationType.MISSION_REMINDER, LocalDateTime.of(2026, 10, 5, 0, 0)))
                .thenReturn(List.of(2L));

        int count = notificationService.sendMissionReminders();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        assertThat(count).isEqualTo(1);
        assertThat(captor.getValue()).singleElement().satisfies(notification -> {
            assertThat(notification.getUser()).isSameAs(user);
            assertThat(notification.getTitle()).isEqualTo("아직 오늘의 굳이를 안 남기셨네요");
            assertThat(notification.getContent()).isEqualTo("혼자 가는 카페에서 창가 자리 앉기 - 잊지 않으셨죠?");
            assertThat(notification.isRead()).isFalse();
        });
    }

    @Test
    void 리마인드를_만들면_미션_리마인드_문구로_푸시_이벤트를_발행한다() {
        when(userMissionRepository.findAllByStatus(UserMissionStatus.IN_PROGRESS)).thenReturn(List.of(
                inProgress(user, "혼자 가는 카페에서 창가 자리 앉기")
        ));
        when(notificationRepository.findUserIdsByTypeAndCreatedAtSince(any(), any())).thenReturn(List.of());

        notificationService.sendMissionReminders();

        ArgumentCaptor<NotificationPushEvent> captor = ArgumentCaptor.forClass(NotificationPushEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().requests()).singleElement().satisfies(request -> {
            assertThat(request.userId()).isEqualTo(1L);
            assertThat(request.title()).isEqualTo("미션 리마인드");
            assertThat(request.body()).isEqualTo("오늘 해본 경험을 사진과 글로 남겨보세요.");
            assertThat(request.data()).containsEntry("type", "MISSION_REMINDER");
        });
    }

    @Test
    void 리마인드_대상이_없으면_푸시_이벤트를_발행하지_않는다() {
        when(userMissionRepository.findAllByStatus(UserMissionStatus.IN_PROGRESS)).thenReturn(List.of());
        when(notificationRepository.findUserIdsByTypeAndCreatedAtSince(any(), any())).thenReturn(List.of());

        int count = notificationService.sendMissionReminders();

        assertThat(count).isZero();
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void 서비스_업데이트는_전체_사용자에게_알림함_알림을_만들고_푸시_이벤트를_발행한다() {
        User other = user(2L, "other@test.com");
        when(userRepository.findAll()).thenReturn(List.of(user, other));

        int count = notificationService.sendServiceUpdate("굳이가 더 편리해졌어요", "캘린더가 생겼어요.");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> notificationCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(notificationCaptor.capture());
        assertThat(count).isEqualTo(2);
        assertThat(notificationCaptor.getValue()).hasSize(2).allSatisfy(notification -> {
            assertThat(notification.getType()).isEqualTo(NotificationType.SERVICE_UPDATE);
            assertThat(notification.getTitle()).isEqualTo("굳이가 더 편리해졌어요");
            assertThat(notification.getContent()).isEqualTo("캘린더가 생겼어요.");
        });

        ArgumentCaptor<NotificationPushEvent> eventCaptor = ArgumentCaptor.forClass(NotificationPushEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().requests())
                .extracting(PushRequest::userId, PushRequest::title, PushRequest::body)
                .containsExactly(
                        tuple(1L, "서비스 업데이트", "굳이가 더 편리해졌어요. 새로운 소식을 확인해보세요."),
                        tuple(2L, "서비스 업데이트", "굳이가 더 편리해졌어요. 새로운 소식을 확인해보세요.")
                );
    }

    @Test
    void 서비스_업데이트_제목을_비우면_기본_제목을_쓴다() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        notificationService.sendServiceUpdate(" ", "내용");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement()
                .extracting(Notification::getTitle).isEqualTo("서비스 업데이트");
    }

    @Test
    void 알림_상세를_보면_읽음_처리된다() {
        Notification notification = Notification.createNotification(user, NotificationType.MISSION_REMINDER, "내용");
        when(notificationRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(notification));

        NotificationResponse result = notificationService.getNotification(EMAIL, 10L);

        assertThat(result.read()).isTrue();
        assertThat(notification.getReadAt()).isEqualTo(NOW.toLocalDateTime());
    }

    @Test
    void 이미_읽은_알림은_처음_읽은_시각을_유지한다() {
        Notification notification = Notification.createNotification(user, NotificationType.MISSION_REMINDER, "내용");
        notification.read(LocalDateTime.of(2026, 10, 1, 9, 0));
        when(notificationRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(notification));

        notificationService.getNotification(EMAIL, 10L);

        assertThat(notification.getReadAt()).isEqualTo(LocalDateTime.of(2026, 10, 1, 9, 0));
    }

    @Test
    void 본인_알림이_아니면_NOTIFICATION_NOT_FOUND() {
        when(notificationRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.getNotification(EMAIL, 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
    }

    private User user(Long id, String email) {
        User created = User.createSocialUser(email, "사용자" + id, "google-" + id);
        ReflectionTestUtils.setField(created, "id", id);
        return created;
    }

    private UserMission inProgress(User owner, String title) {
        return UserMission.startMission(owner, Mission.createMission(title, "설명", DayType.ALL, Season.ALL));
    }
}