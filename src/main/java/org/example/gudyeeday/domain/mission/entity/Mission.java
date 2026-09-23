package org.example.gudyeeday.domain.mission.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gudyeeday.common.entity.BaseEntity;
import org.example.gudyeeday.domain.mission.enums.DayType;
import org.example.gudyeeday.domain.mission.enums.Season;

@Entity
@Table(name = "mission")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
}
