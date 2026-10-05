package org.example.gudyeeday.domain.notification.repository;

import org.example.gudyeeday.config.TimeConfig;
import org.example.gudyeeday.domain.notification.entity.Notification;
import org.example.gudyeeday.domain.notification.enums.NotificationType;
import org.example.gudyeeday.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TimeConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        // user 테이블명이 H2 예약어라 NON_KEYWORDS 지정
        "spring.datasource.url=jdbc:h2:mem:notification;MODE=MySQL;NON_KEYWORDS=USER",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database=h2",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TestEntityManager em;

    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        user = em.persist(User.createSocialUser("user@test.com", "사용자", "google-1"));
        otherUser = em.persist(User.createSocialUser("other@test.com", "다른사용자", "google-2"));
    }

    @Test
    void 목록은_본인_알림만_최신순으로_조회된다() {
        Notification older = persist(user, LocalDateTime.of(2026, 10, 4, 20, 0));
        Notification newer = persist(user, LocalDateTime.of(2026, 10, 5, 20, 0));
        persist(otherUser, LocalDateTime.of(2026, 10, 5, 21, 0));

        assertThat(notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(user.getId()))
                .extracting(Notification::getId).containsExactly(newer.getId(), older.getId());
    }

    @Test
    void 안_읽은_알림이_있는지_확인한다() {
        Notification notification = persist(user, LocalDateTime.of(2026, 10, 5, 20, 0));
        assertThat(notificationRepository.existsByUserIdAndReadAtIsNull(user.getId())).isTrue();

        notification.read(LocalDateTime.of(2026, 10, 5, 21, 0));
        em.flush();

        assertThat(notificationRepository.existsByUserIdAndReadAtIsNull(user.getId())).isFalse();
    }

    @Test
    void 기준_시각_이후_알림을_받은_사용자를_조회한다() {
        persist(user, LocalDateTime.of(2026, 10, 4, 20, 0));
        persist(otherUser, LocalDateTime.of(2026, 10, 5, 20, 0));

        assertThat(notificationRepository.findUserIdsByTypeAndCreatedAtSince(
                NotificationType.MISSION_REMINDER, LocalDateTime.of(2026, 10, 5, 0, 0)))
                .containsExactly(otherUser.getId());
    }

    // created_at은 감사 기능이 저장 시 현재 시각으로 채우므로 저장 후 덮어씀
    private Notification persist(User owner, LocalDateTime createdAt) {
        Notification notification = em.persist(Notification.createNotification(owner, NotificationType.MISSION_REMINDER, "내용"));
        em.flush();
        em.getEntityManager().createQuery("update Notification n set n.createdAt = :createdAt where n.id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", notification.getId())
                .executeUpdate();
        ReflectionTestUtils.setField(notification, "createdAt", createdAt);
        return notification;
    }
}
