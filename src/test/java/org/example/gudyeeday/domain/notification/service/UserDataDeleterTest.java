package org.example.gudyeeday.domain.notification.service;

import org.example.gudyeeday.common.s3.S3Uploader;
import org.example.gudyeeday.config.TimeConfig;
import org.example.gudyeeday.domain.complete.entity.CompleteMission;
import org.example.gudyeeday.domain.complete.repository.CompleteMissionRepository;
import org.example.gudyeeday.domain.home.entity.WeeklyPhoto;
import org.example.gudyeeday.domain.home.repository.WeeklyPhotoRepository;
import org.example.gudyeeday.domain.mission.entity.Mission;
import org.example.gudyeeday.domain.mission.entity.MissionBookmark;
import org.example.gudyeeday.domain.mission.entity.UserMission;
import org.example.gudyeeday.domain.mission.enums.DayType;
import org.example.gudyeeday.domain.mission.enums.Season;
import org.example.gudyeeday.domain.mission.repository.MissionBookmarkRepository;
import org.example.gudyeeday.domain.mission.repository.MissionRepository;
import org.example.gudyeeday.domain.mission.repository.UserMissionRepository;
import org.example.gudyeeday.domain.notification.entity.DeviceToken;
import org.example.gudyeeday.domain.notification.entity.Notification;
import org.example.gudyeeday.domain.notification.enums.NotificationType;
import org.example.gudyeeday.domain.notification.repository.DeviceTokenRepository;
import org.example.gudyeeday.domain.notification.repository.NotificationRepository;
import org.example.gudyeeday.domain.user.entity.EmailVerification;
import org.example.gudyeeday.domain.user.entity.RefreshToken;
import org.example.gudyeeday.domain.user.entity.User;
import org.example.gudyeeday.domain.user.repository.EmailVerificationRepository;
import org.example.gudyeeday.domain.user.repository.RefreshTokenRepository;
import org.example.gudyeeday.domain.user.repository.UserRepository;
import org.example.gudyeeday.domain.user.service.UserDataDeleter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

// 탈퇴 삭제 쿼리가 외래키 순서대로 실제로 지워지는지 H2로 확인
@DataJpaTest
@Import(TimeConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        // user 테이블명이 H2 예약어라 NON_KEYWORDS 지정
        "spring.datasource.url=jdbc:h2:mem:withdraw;MODE=MySQL;NON_KEYWORDS=USER",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database=h2",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class UserDataDeleterTest {

    @Autowired private TestEntityManager em;
    @Autowired private CompleteMissionRepository completeMissionRepository;
    @Autowired private WeeklyPhotoRepository weeklyPhotoRepository;
    @Autowired private UserMissionRepository userMissionRepository;
    @Autowired private MissionBookmarkRepository missionBookmarkRepository;
    @Autowired private MissionRepository missionRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private DeviceTokenRepository deviceTokenRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private EmailVerificationRepository emailVerificationRepository;
    @Autowired private UserRepository userRepository;

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    @Test
    void 탈퇴하면_본인과_나만의_굳이에_연결된_데이터만_모두_삭제된다() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 19, 12, 0);

        User user = em.persist(User.createLocalUser("bye@test.com", "encoded", "탈퇴자"));
        User other = em.persist(User.createLocalUser("stay@test.com", "encoded", "남는사람"));
        Mission baseMission = em.persist(Mission.createMission("기본 미션", "설명", DayType.ALL, Season.ALL));
        Mission customMission = em.persist(Mission.createCustomMission(user, "나만의 굳이", "설명"));

        // 탈퇴자 데이터
        UserMission done = UserMission.startMission(user, baseMission);
        done.complete(now);
        em.persist(done);
        em.persist(CompleteMission.create(user, done, "https://bucket/complete-mission/a.jpg", "카페", "좋았다", false));
        em.persist(WeeklyPhoto.createWeeklyPhoto(user, now.toLocalDate(), "https://bucket/complete-mission/a.jpg"));
        em.persist(UserMission.startMission(user, customMission));
        em.persist(MissionBookmark.createMissionBookmark(user, baseMission, now));
        em.persist(MissionBookmark.createMissionBookmark(user, customMission, now));
        em.persist(Notification.createNotification(user, NotificationType.DAILY_MISSION, "내용"));
        em.persist(DeviceToken.create(user, "device-token"));
        em.persist(RefreshToken.createRefreshToken("hashed", user, now.plusDays(7)));
        em.persist(EmailVerification.createEmailVerification("bye@test.com", "code", now.plusMinutes(5)));

        // 남아야 하는 다른 사용자 데이터
        UserMission otherDone = UserMission.startMission(other, baseMission);
        otherDone.complete(now);
        em.persist(otherDone);
        em.persist(CompleteMission.create(other, otherDone, "https://bucket/complete-mission/b.jpg", null, null, false));
        em.persist(WeeklyPhoto.createWeeklyPhoto(other, LocalDate.of(2026, 11, 19), "https://bucket/complete-mission/b.jpg"));
        em.persist(MissionBookmark.createMissionBookmark(other, baseMission, now));
        em.persist(Notification.createNotification(other, NotificationType.DAILY_MISSION, "내용"));
        em.persist(DeviceToken.create(other, "other-device-token"));
        em.flush();
        em.clear();

        Long userId = user.getId();
        newDeleter().deleteAll(userId, "bye@test.com");
        em.flush();
        em.clear();

        assertThat(userRepository.findById(userId)).isEmpty();
        assertThat(userRepository.findById(other.getId())).isPresent();

        assertThat(completeMissionRepository.findAll()).extracting(cm -> cm.getUser().getId()).containsOnly(other.getId());
        assertThat(weeklyPhotoRepository.findAll()).extracting(wp -> wp.getUser().getId()).containsOnly(other.getId());
        assertThat(userMissionRepository.findAll()).extracting(um -> um.getUser().getId()).containsOnly(other.getId());
        assertThat(missionBookmarkRepository.findAll()).extracting(b -> b.getUser().getId()).containsOnly(other.getId());
        assertThat(notificationRepository.findAll()).extracting(n -> n.getUser().getId()).containsOnly(other.getId());
        assertThat(deviceTokenRepository.findAll()).extracting(DeviceToken::getToken).containsOnly("other-device-token");
        assertThat(refreshTokenRepository.findAll()).isEmpty();
        assertThat(emailVerificationRepository.findByEmail("bye@test.com")).isEmpty();

        // 기본 미션은 남고, 나만의 굳이만 삭제
        assertThat(missionRepository.findAll()).extracting(Mission::getTitle).containsExactly("기본 미션");

        // 커밋 후 지울 S3 사진: 중복 없이 탈퇴자 사진만
        ArgumentCaptor<UserDataDeleter.UserImagesDeleteEvent> captor =
                ArgumentCaptor.forClass(UserDataDeleter.UserImagesDeleteEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().imageUrls()).containsExactly("https://bucket/complete-mission/a.jpg");
    }

    private UserDataDeleter newDeleter() {
        return new UserDataDeleter(
                completeMissionRepository, weeklyPhotoRepository, userMissionRepository,
                missionBookmarkRepository, missionRepository, notificationRepository,
                deviceTokenRepository, refreshTokenRepository, emailVerificationRepository,
                userRepository, eventPublisher, mock(S3Uploader.class)
        );
    }
}