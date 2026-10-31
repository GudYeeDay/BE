package org.example.gudyeeday.domain.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gudyeeday.common.entity.BaseEntity;
import org.example.gudyeeday.domain.user.entity.User;

// 푸시를 받을 기기의 FCM 토큰 (한 사용자가 여러 기기를 쓸 수 있음)
@Entity
@Table(
        name = "device_token",
        uniqueConstraints = {@UniqueConstraint(name = "uk_device_token_token", columnNames = "token")},
        indexes = {@Index(name = "idx_device_token_user", columnList = "user_id")}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_token_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, length = 512)
    private String token;

    private DeviceToken(User user, String token) {
        this.user = user;
        this.token = token;
    }

    public static DeviceToken create(User user, String token) {
        return new DeviceToken(user, token);
    }

    // 같은 기기에서 다른 계정으로 로그인하면 토큰 주인을 바꿈
    public void changeOwner(User user) {
        this.user = user;
    }
}