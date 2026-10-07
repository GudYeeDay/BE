package org.example.gudyeeday.domain.complete.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.gudyeeday.common.entity.BaseEntity;
import org.example.gudyeeday.domain.mission.entity.UserMission;
import org.example.gudyeeday.domain.user.entity.User;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "complete_mission")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class CompleteMission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "complete_mission_id")
    private Long completeMissionId;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "content", length = 500)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_mission_id", nullable = false, unique = true)
    private UserMission userMission;

    public static CompleteMission create(User user, UserMission userMission, String imageUrl,
                                         String location, String content) {
        return CompleteMission.builder()
                .user(user)
                .userMission(userMission)
                .imageUrl(imageUrl)
                .location(location)
                .content(content)
                .build();
    }

    public void updateRecord(String location, String content) {
        this.location = location;
        this.content = content;
    }

    public void updateImage(String imageUrl) {
        this.imageUrl = imageUrl;
    }

}