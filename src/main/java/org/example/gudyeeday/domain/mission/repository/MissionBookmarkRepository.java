package org.example.gudyeeday.domain.mission.repository;

import org.example.gudyeeday.domain.mission.entity.MissionBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MissionBookmarkRepository extends JpaRepository<MissionBookmark, Long> {

    Optional<MissionBookmark> findByUserIdAndMissionId(Long userId, Long missionId);

    // 보관함 목록 (최근 저장순)
    @Query("""
            select b from MissionBookmark b
            join fetch b.mission
            where b.user.id = :userId
            order by coalesce(b.savedAt, b.createdAt) desc, b.id desc
            """)
    List<MissionBookmark> findAllWithMissionByUserId(@Param("userId") Long userId);

    @Query("""
            select b from MissionBookmark b
            join fetch b.mission
            where b.id = :bookmarkId
              and b.user.id = :userId
            """)
    Optional<MissionBookmark> findWithMissionByIdAndUserId(@Param("bookmarkId") Long bookmarkId, @Param("userId") Long userId);
}
