package org.example.gudyeeday.domain.mission.repository;

import org.example.gudyeeday.config.TimeConfig;
import org.example.gudyeeday.domain.mission.entity.Mission;
import org.example.gudyeeday.domain.mission.entity.MissionBookmark;
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
        "spring.datasource.url=jdbc:h2:mem:bookmark;MODE=MySQL;NON_KEYWORDS=USER",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database=h2",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class MissionBookmarkRepositoryTest {

    @Autowired
    private MissionBookmarkRepository missionBookmarkRepository;

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
    void 보관함_목록은_본인_것만_최근_저장순으로_조회된다() {
        MissionBookmark first = persistBookmark(user, LocalDateTime.of(2026, 8, 29, 9, 0));
        MissionBookmark second = persistBookmark(user, LocalDateTime.of(2026, 9, 1, 9, 0));
        persistBookmark(otherUser, LocalDateTime.of(2026, 9, 2, 9, 0));
        em.flush();
        em.clear();

        List<MissionBookmark> result = missionBookmarkRepository.findAllWithMissionByUserId(user.getId());

        assertThat(result).extracting(MissionBookmark::getId).containsExactly(second.getId(), first.getId());
        assertThat(result.get(0).getMission().getTitle()).isEqualTo("제목");
    }

    @Test
    void 다른_사용자의_보관함_항목은_조회되지_않는다() {
        MissionBookmark others = persistBookmark(otherUser, LocalDateTime.now());

        assertThat(missionBookmarkRepository.findWithMissionByIdAndUserId(others.getId(), user.getId())).isEmpty();
        assertThat(missionBookmarkRepository.findWithMissionByIdAndUserId(others.getId(), otherUser.getId())).isPresent();
    }

    private MissionBookmark persistBookmark(User owner, LocalDateTime savedAt) {
        Mission mission = em.persist(Mission.createMission("제목", "설명", DayType.ALL, Season.ALL));
        return em.persist(MissionBookmark.createMissionBookmark(owner, mission, savedAt));
    }
}
