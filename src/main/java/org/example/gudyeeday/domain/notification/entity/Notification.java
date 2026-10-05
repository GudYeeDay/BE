package org.example.gudyeeday.domain.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.gudyeeday.common.entity.BaseEntity;
import org.example.gudyeeday.domain.notification.enums.NotificationType;
import org.example.gudyeeday.domain.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification", indexes = {@Index(name = "idx_notification_user_created", columnList = "user_id, created_at")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    // 읽은 시각 (null이면 안 읽음)
    @Column(name = "read_at")
    private LocalDateTime readAt;

    public static Notification createNotification(User user, NotificationType type, String content) {
        return Notification.builder()
                .user(user)
                .type(type)
                .title(type.getTitle())
                .content(content)
                .build();
    }

    public boolean isRead() {
        return readAt != null;
    }

    // 처음 읽은 시각만 기록
    public void read(LocalDateTime readAt) {
        if (this.readAt == null) {
            this.readAt = readAt;
        }
    }
}
