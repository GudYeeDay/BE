package org.example.gudyeeday.domain.home.repository;

import org.example.gudyeeday.config.TimeConfig;
import org.example.gudyeeday.domain.complete.entity.CompleteMission;
import org.example.gudyeeday.domain.complete.repository.CompleteMissionRepository;
import org.example.gudyeeday.domain.home.entity.WeeklyPhoto;
import org.example.gudyeeday.domain.mission.entity.Mission;
import org.example.gudyeeday.domain.mission.entity.UserMission;
import org.example.gudyeeday.domain.mission.enums.DayType;
import org.example.gudyeeday.domain.mission.enums.Season;
import org.example.gudyeeday.domain.mission.repository.UserMissionRepository;
import org.example.gudyeeday.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TimeConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        // user 테이블명이 H2 예약어라 NON_KEYWORDS 지정
        "spring.datasource.url=jdbc:h2:mem:home;MODE=MySQL;NON_KEYWORDS=USER",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database=h2",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class HomeRepositoryTest {

    @Autowired
    private WeeklyPhotoRepository weeklyPhotoRepository;

    @Autowired
    private UserMissionRepository userMissionRepository;

    @Autowired
    private CompleteMissionRepository completeMissionRepository;

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
    void 주간_사진은_본인의_기간_내_사진만_조회된다() {
        em.persist(WeeklyPhoto.createWeeklyPhoto(user, LocalDate.of(2026, 9, 27), "last-week.jpg"));
        em.persist(WeeklyPhoto.createWeeklyPhoto(user, LocalDate.of(2026, 9, 28), "mon.jpg"));
        em.persist(WeeklyPhoto.createWeeklyPhoto(user, LocalDate.of(2026, 10, 4), "sun.jpg"));
        em.persist(WeeklyPhoto.createWeeklyPhoto(otherUser, LocalDate.of(2026, 9, 30), "other.jpg"));
        em.flush();
        em.clear();

        List<WeeklyPhoto> result = weeklyPhotoRepository.findByUserIdAndPhotoDateBetween(
                user.getId(), LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 4));

        assertThat(result).extracting(WeeklyPhoto::getImageUrl).containsExactlyInAnyOrder("mon.jpg", "sun.jpg");
    }

    @Test
    void 완료_시각은_본인의_기간_내_완료한_미션만_조회된다() {
        Mission mission = em.persist(Mission.createMission("미션", "설명", DayType.ALL, Season.ALL));
        persistCompleted(user, mission, LocalDateTime.of(2026, 9, 30, 23, 59));
        persistCompleted(user, mission, LocalDateTime.of(2026, 10, 1, 0, 0));
        persistCompleted(user, mission, LocalDateTime.of(2026, 10, 31, 23, 59));
        persistCompleted(user, mission, LocalDateTime.of(2026, 11, 1, 0, 0));
        persistCompleted(otherUser, mission, LocalDateTime.of(2026, 10, 2, 9, 0));
        em.persist(UserMission.startMission(user, mission));
        em.flush();
        em.clear();

        List<LocalDateTime> result = userMissionRepository.findCompletedAtBetween(
                user.getId(), LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 11, 1, 0, 0));

        assertThat(result).containsExactlyInAnyOrder(
                LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 10, 31, 23, 59)
        );
    }

    @Test
    void 지난_기록은_본인이_기준_시각_이전에_완료한_것만_조회된다() {
        Mission mission = em.persist(Mission.createMission("미션", "설명", DayType.ALL, Season.ALL));
        CompleteMission lastSunday = persistRecord(user, mission, LocalDateTime.of(2026, 9, 27, 23, 59));
        CompleteMission older = persistRecord(user, mission, LocalDateTime.of(2026, 8, 1, 9, 0));
        persistRecord(user, mission, LocalDateTime.of(2026, 9, 28, 0, 0));
        persistRecord(otherUser, mission, LocalDateTime.of(2026, 9, 1, 9, 0));
        em.flush();
        em.clear();

        List<Long> ids = completeMissionRepository.findIdsByUserIdAndCompletedAtBefore(
                user.getId(), LocalDateTime.of(2026, 9, 28, 0, 0));
        List<CompleteMission> records = completeMissionRepository.findWithMissionByIdIn(ids);

        assertThat(ids).containsExactlyInAnyOrder(lastSunday.getCompleteMissionId(), older.getCompleteMissionId());
        assertThat(records).extracting(record -> record.getUserMission().getMission().getTitle()).containsOnly("미션");
    }

    private UserMission persistCompleted(User owner, Mission mission, LocalDateTime completedAt) {
        UserMission userMission = UserMission.startMission(owner, mission);
        userMission.complete(completedAt);
        return em.persist(userMission);
    }

    private CompleteMission persistRecord(User owner, Mission mission, LocalDateTime completedAt) {
        UserMission userMission = persistCompleted(owner, mission, completedAt);
        return em.persist(CompleteMission.create(owner, userMission, "photo.jpg", "위치", "내용"));
    }
}
