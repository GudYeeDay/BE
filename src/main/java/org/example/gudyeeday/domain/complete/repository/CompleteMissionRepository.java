package org.example.gudyeeday.domain.complete.repository;

import org.example.gudyeeday.domain.complete.entity.CompleteMission;
import org.example.gudyeeday.domain.mission.entity.UserMission;
import org.example.gudyeeday.domain.mission.enums.UserMissionStatus;
import org.example.gudyeeday.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompleteMissionRepository extends JpaRepository<CompleteMission, Long> {

    boolean existsCompleteMissionByUserMission(UserMission userMission);

    @Query("""
            SELECT cm FROM CompleteMission cm
            JOIN FETCH cm.userMission um
            JOIN FETCH um.mission
            WHERE cm.user = :user
              AND um.completedAt >= :start
              AND um.completedAt < :end
            ORDER BY um.completedAt DESC
            """)
    List<CompleteMission> findTodayCompleteMissions(@Param("user") User user,
                                                    @Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end);

    // 완료 시각이 before 이전인 기록 id
    @Query("""
            SELECT cm.completeMissionId FROM CompleteMission cm
            JOIN cm.userMission um
            WHERE cm.user.id = :userId
              AND um.completedAt < :before
            """)
    List<Long> findIdsByUserIdAndCompletedAtBefore(@Param("userId") Long userId,
                                                   @Param("before") LocalDateTime before);

    @Query("""
            SELECT cm FROM CompleteMission cm
            JOIN FETCH cm.userMission um
            JOIN FETCH um.mission
            WHERE cm.completeMissionId IN :ids
            """)
    List<CompleteMission> findWithMissionByIdIn(@Param("ids") Collection<Long> ids);

}
