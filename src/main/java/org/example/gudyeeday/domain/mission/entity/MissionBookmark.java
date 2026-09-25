package org.example.gudyeeday.domain.mission.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.gudyeeday.common.entity.BaseEntity;
import org.example.gudyeeday.domain.user.entity.User;

@Entity
@Table(name = "mission_bookmark", uniqueConstraints = {@UniqueConstraint(name = "uk_mission_bookmark_user_mission", columnNames = {"user_id", "mission_id"})})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class MissionBookmark extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mission_bookmark_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    public static MissionBookmark createMissionBookmark(User user, Mission mission) {
        return MissionBookmark.builder()
                .user(user)
                .mission(mission)
                .build();
    }
}
