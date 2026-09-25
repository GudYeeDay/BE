package org.example.gudyeeday.domain.mission.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.gudyeeday.common.entity.BaseEntity;
import org.example.gudyeeday.domain.user.entity.User;

import java.time.LocalDateTime;

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

    // 보관함에 저장한 시각 (삭제 후 다시 저장하면 갱신)
    // 컬럼 추가 전 저장된 row는 null이라 created_at으로 대체
    @Column(name = "saved_at")
    private LocalDateTime savedAt;

    // 보관함에서 삭제한 시각 (소프트 삭제, null이면 보관함에 있음)
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static MissionBookmark createMissionBookmark(User user, Mission mission, LocalDateTime savedAt) {
        return MissionBookmark.builder()
                .user(user)
                .mission(mission)
                .savedAt(savedAt)
                .build();
    }

    public LocalDateTime getSavedAt() {
        return savedAt != null ? savedAt : getCreatedAt();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    // 보관함에서 삭제 (소프트 삭제)
    public void delete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    // 삭제했던 미션을 다시 저장
    public void restore(LocalDateTime savedAt) {
        this.deletedAt = null;
        this.savedAt = savedAt;
    }
}
