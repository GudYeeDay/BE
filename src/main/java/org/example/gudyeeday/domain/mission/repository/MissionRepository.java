package org.example.gudyeeday.domain.mission.repository;

import org.example.gudyeeday.domain.mission.entity.Mission;
import org.example.gudyeeday.domain.mission.enums.DayType;
import org.example.gudyeeday.domain.mission.enums.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    // 요일/계절 조건에 맞고, 사용자가 보관함에 저장하지 않았으며 진행중이거나 완료한 적 없는 미션
    // (그만둔 미션은 진행 기록이 삭제되므로 다시 추천됨)
    @Query("""
            select m from Mission m
            where m.dayType in :dayTypes
              and m.season in :seasons
              and not exists (
                  select 1 from MissionBookmark b
                  where b.mission = m and b.user.id = :userId
              )
              and not exists (
                  select 1 from UserMission um
                  where um.mission = m and um.user.id = :userId
              )
            """)
    List<Mission> findRecommendCandidates(
            @Param("userId") Long userId,
            @Param("dayTypes") Collection<DayType> dayTypes,
            @Param("seasons") Collection<Season> seasons
    );
}
