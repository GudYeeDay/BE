package org.example.gudyeeday.domain.mission.repository;

import org.example.gudyeeday.config.TimeConfig;
import org.example.gudyeeday.domain.mission.entity.Mission;
import org.example.gudyeeday.domain.mission.entity.MissionBookmark;
import org.example.gudyeeday.domain.mission.entity.UserMission;
import org.example.gudyeeday.domain.mission.enums.DayType;
import org.example.gudyeeday.domain.mission.enums.Season;
import org.example.gudyeeday.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TimeConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        // user 테이블명이 H2 예약어라 NON_KEYWORDS 지정
        "spring.datasource.url=jdbc:h2:mem:mission;MODE=MySQL;NON_KEYWORDS=USER",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database=h2",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class MissionRepositoryTest {

    private static final List<DayType> WEEKDAY = List.of(DayType.ALL, DayType.WEEKDAY);
    private static final List<Season> FALL = List.of(Season.ALL, Season.FALL);

    @Autowired
    private MissionRepository missionRepository;

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
    void 요일과_계절_조건에_맞는_미션만_후보가_된다() {
        Mission all = persistMission(DayType.ALL, Season.ALL);
        Mission weekdayFall = persistMission(DayType.WEEKDAY, Season.FALL);
        persistMission(DayType.WEEKEND, Season.ALL);
        persistMission(DayType.ALL, Season.SUMMER);

        List<Mission> candidates = missionRepository.findRecommendCandidates(user.getId(), WEEKDAY, FALL);

        assertThat(candidates).containsExactlyInAnyOrder(all, weekdayFall);
    }

    @Test
    void 보관함에_저장한_미션은_제외된다() {
        Mission bookmarked = persistMission(DayType.ALL, Season.ALL);
        Mission other = persistMission(DayType.ALL, Season.ALL);
        em.persist(MissionBookmark.createMissionBookmark(user, bookmarked, LocalDateTime.now()));

        List<Mission> candidates = missionRepository.findRecommendCandidates(user.getId(), WEEKDAY, FALL);

        assertThat(candidates).containsExactly(other);
    }

    @Test
    void 보관함에서_삭제한_미션은_다시_추천된다() {
        Mission mission = persistMission(DayType.ALL, Season.ALL);
        MissionBookmark bookmark = MissionBookmark.createMissionBookmark(user, mission, LocalDateTime.now());
        bookmark.delete(LocalDateTime.now());
        em.persist(bookmark);

        assertThat(missionRepository.findRecommendCandidates(user.getId(), WEEKDAY, FALL)).containsExactly(mission);
    }

    @Test
    void 진행중인_미션은_제외된다() {
        Mission inProgress = persistMission(DayType.ALL, Season.ALL);
        Mission other = persistMission(DayType.ALL, Season.ALL);
        em.persist(UserMission.startMission(user, inProgress));

        List<Mission> candidates = missionRepository.findRecommendCandidates(user.getId(), WEEKDAY, FALL);

        assertThat(candidates).containsExactly(other);
    }

    @Test
    void 완료한_미션은_제외된다() {
        Mission completed = persistMission(DayType.ALL, Season.ALL);
        Mission other = persistMission(DayType.ALL, Season.ALL);
        UserMission userMission = UserMission.startMission(user, completed);
        userMission.complete(LocalDateTime.now());
        em.persist(userMission);

        List<Mission> candidates = missionRepository.findRecommendCandidates(user.getId(), WEEKDAY, FALL);

        assertThat(candidates).containsExactly(other);
    }

    @Test
    void 다른_사용자의_보관함과_진행_기록은_영향을_주지_않는다() {
        Mission mission = persistMission(DayType.ALL, Season.ALL);
        em.persist(MissionBookmark.createMissionBookmark(otherUser, mission, LocalDateTime.now()));
        UserMission userMission = UserMission.startMission(otherUser, mission);
        userMission.complete(LocalDateTime.now());
        em.persist(userMission);

        List<Mission> candidates = missionRepository.findRecommendCandidates(user.getId(), WEEKDAY, FALL);

        assertThat(candidates).containsExactly(mission);
    }

    @Test
    void 나만의_굳이_미션은_만든_사람을_포함해_누구에게도_추천되지_않는다() {
        Mission basic = persistMission(DayType.ALL, Season.ALL);
        em.persist(Mission.createCustomMission(user, "내 미션", "설명"));
        em.persist(Mission.createCustomMission(otherUser, "남의 미션", "설명"));

        assertThat(missionRepository.findRecommendCandidates(user.getId(), WEEKDAY, FALL)).containsExactly(basic);
    }

    private Mission persistMission(DayType dayType, Season season) {
        return em.persist(Mission.createMission("제목", "설명", dayType, season));
    }
}
