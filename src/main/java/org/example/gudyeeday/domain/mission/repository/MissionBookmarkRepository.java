package org.example.gudyeeday.domain.mission.repository;

import org.example.gudyeeday.domain.mission.entity.MissionBookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MissionBookmarkRepository extends JpaRepository<MissionBookmark, Long> {

    boolean existsByUserIdAndMissionId(Long userId, Long missionId);

    Optional<MissionBookmark> findByUserIdAndMissionId(Long userId, Long missionId);
}
