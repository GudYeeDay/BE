package org.example.gudyeeday.domain.mission.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.gudyeeday.common.entity.BaseEntity;
import org.example.gudyeeday.domain.mission.enums.DayType;
import org.example.gudyeeday.domain.mission.enums.Season;
import org.example.gudyeeday.domain.user.entity.User;

@Entity
@Table(name = "mission")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Mission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mission_id")
    private Long id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    // 추천 요일 구분 (ALL: 요일 무관)
    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false, length = 20)
    private DayType dayType;

    // 추천 계절 구분 (ALL: 계절 무관)
    @Enumerated(EnumType.STRING)
    @Column(name = "season", nullable = false, length = 20)
    private Season season;

    // 나만의 굳이 미션 작성자 (null: 기본 제공 미션)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    public static Mission createMission(String title, String description, DayType dayType, Season season) {
        return Mission.builder()
                .title(title)
                .description(description)
                .dayType(dayType)
                .season(season)
                .build();
    }

    // 나만의 굳이 미션 생성 (추천 대상 아님)
    public static Mission createCustomMission(User creator, String title, String description) {
        return Mission.builder()
                .title(title)
                .description(description)
                .dayType(DayType.ALL)
                .season(Season.ALL)
                .creator(creator)
                .build();
    }

    public boolean isCustom() {
        return creator != null;
    }

    // 기본 제공 미션이거나 본인이 만든 미션만 사용 가능
    public boolean isAccessibleBy(User user) {
        return creator == null || creator.getId().equals(user.getId());
    }
}
