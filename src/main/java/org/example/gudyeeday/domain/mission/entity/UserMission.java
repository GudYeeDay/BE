package org.example.gudyeeday.domain.mission.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.gudyeeday.common.entity.BaseEntity;
import org.example.gudyeeday.domain.mission.enums.UserMissionStatus;
import org.example.gudyeeday.domain.user.entity.User;

@Entity
@Table(name = "user_mission")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class UserMission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_mission_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserMissionStatus status;

    // 미션 시작
    public static UserMission startMission(User user, Mission mission) {
        return UserMission.builder()
                .user(user)
                .mission(mission)
                .status(UserMissionStatus.IN_PROGRESS)
                .build();
    }
}
