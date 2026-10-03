package org.example.gudyeeday.domain.mission.repository;

import org.example.gudyeeday.domain.mission.entity.UserMission;
import org.example.gudyeeday.domain.mission.enums.UserMissionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserMissionRepository extends JpaRepository<UserMission, Long> {

    boolean existsByUserIdAndStatus(Long userId, UserMissionStatus status);

    @EntityGraph(attributePaths = "mission")
    Optional<UserMission> findByUserIdAndStatus(Long userId, UserMissionStatus status);

    List<UserMission> findByUserIdAndMissionIdIn(Long userId, Collection<Long> missionIds);

    // 기간 [from, to) 내 완료 시각 목록
    @Query("""
            select um.completedAt from UserMission um
            where um.user.id = :userId
              and um.status = org.example.gudyeeday.domain.mission.enums.UserMissionStatus.COMPLETED
              and um.completedAt >= :from
              and um.completedAt < :to
            """)
    List<LocalDateTime> findCompletedAtBetween(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
