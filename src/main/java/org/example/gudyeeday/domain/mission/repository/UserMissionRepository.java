package org.example.gudyeeday.domain.mission.repository;

import org.example.gudyeeday.domain.mission.entity.UserMission;
import org.example.gudyeeday.domain.mission.enums.UserMissionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserMissionRepository extends JpaRepository<UserMission, Long> {

    boolean existsByUserIdAndStatus(Long userId, UserMissionStatus status);

    @EntityGraph(attributePaths = "mission")
    Optional<UserMission> findByUserIdAndStatus(Long userId, UserMissionStatus status);

    List<UserMission> findByUserIdAndMissionIdIn(Long userId, Collection<Long> missionIds);
}
