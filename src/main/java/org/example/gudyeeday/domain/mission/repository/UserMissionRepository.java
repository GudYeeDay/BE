package org.example.gudyeeday.domain.mission.repository;

import org.example.gudyeeday.domain.mission.entity.UserMission;
import org.example.gudyeeday.domain.mission.enums.UserMissionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @EntityGraph(attributePaths = {"user", "mission"})
    List<UserMission> findAllByStatus(UserMissionStatus status);

    Long countByUserId(Long userId);

    Long countByUserIdAndStatus(Long userId, UserMissionStatus status);

    // 회원 탈퇴: 내 진행 기록 + 내가 만든 나만의 굳이를 진행한 기록 삭제
    @Modifying
    @Query("""
            delete from UserMission um
            where um.user.id = :userId
               or um.mission.id in (select m.id from Mission m where m.creator.id = :userId)
            """)
    int deleteAllForWithdraw(@Param("userId") Long userId);
}
