package org.example.gudyeeday.domain.home.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.gudyeeday.common.entity.BaseEntity;
import org.example.gudyeeday.domain.user.entity.User;

import java.time.LocalDate;

// 홈 상단 주차별 낭만 기록 사진 (사용자당 하루 1장)
// 지난 주 사진도 삭제하지 않고 남겨두며, 홈에서는 이번 주 범위만 조회
@Entity
@Table(name = "weekly_photo", uniqueConstraints = {@UniqueConstraint(name = "uk_weekly_photo_user_date", columnNames = {"user_id", "photo_date"})})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class WeeklyPhoto extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weekly_photo_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 사진을 올린 날짜 (KST)
    @Column(name = "photo_date", nullable = false)
    private LocalDate photoDate;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    public static WeeklyPhoto createWeeklyPhoto(User user, LocalDate photoDate, String imageUrl) {
        return WeeklyPhoto.builder()
                .user(user)
                .photoDate(photoDate)
                .imageUrl(imageUrl)
                .build();
    }

    // 같은 날 다시 올리면 사진 교체
    public void changeImage(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
