package org.example.gudyeeday.domain.notification.repository;

import org.example.gudyeeday.domain.notification.entity.Notification;
import org.example.gudyeeday.domain.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 알림함 목록 (최신순)
    List<Notification> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndReadAtIsNull(Long userId);

    // since 이후 해당 종류의 알림을 받은 사용자 (중복 발송 방지용)
    @Query("""
            select n.user.id from Notification n
            where n.type = :type
              and n.createdAt >= :since
            """)
    List<Long> findUserIdsByTypeAndCreatedAtSince(@Param("type") NotificationType type,
                                                  @Param("since") LocalDateTime since);
}
