package org.example.gudyeeday.domain.notification.repository;

import org.example.gudyeeday.domain.notification.entity.DeviceToken;
import org.example.gudyeeday.domain.notification.push.PushTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    // 푸시 보낼 사용자들의 기기 토큰
    @Query("""
            select new org.example.gudyeeday.domain.notification.push.PushTarget(d.user.id, d.token)
            from DeviceToken d
            where d.user.id in :userIds
            """)
    List<PushTarget> findPushTargetsByUserIdIn(@Param("userIds") Collection<Long> userIds);

    long deleteByUserIdAndToken(Long userId, String token);

    long deleteByTokenIn(Collection<String> tokens);
}