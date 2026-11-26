package org.example.gudyeeday.domain.complete.repository;

import org.example.gudyeeday.domain.complete.entity.CompleteMission;
import org.example.gudyeeday.domain.mission.entity.UserMission;
import org.example.gudyeeday.domain.mission.enums.UserMissionStatus;
import org.example.gudyeeday.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompleteMissionRepository extends JpaRepository<CompleteMission, Long> {

    CompleteMission findByCompleteMissionId(Long completeMissionId);

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
    List<CompleteMission> findDailyCompleteMissions(@Param("user") User user,
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

    // 기간 [start, end) 안에 남긴 기록의 완료 시각 (홈 이번 달 기록 일수)
    @Query("""
            SELECT um.completedAt FROM CompleteMission cm
            JOIN cm.userMission um
            WHERE cm.user.id = :userId
              AND um.completedAt >= :start
              AND um.completedAt < :end
            """)
    List<LocalDateTime> findCompletedAtByUserIdBetween(@Param("userId") Long userId,
                                                       @Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end);

    Long countByUserId(Long userId);

    @Query("""
        select cm from CompleteMission cm
        join fetch cm.userMission um
        where cm.user.id = :userId
          and um.completedAt >= :start
          and um.completedAt < :end
        order by um.completedAt asc
        """)
    List<CompleteMission> findAllByUserIdAndCreatedAtBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // 회원 탈퇴: 지울 기록 사진 주소 (내 기록 + 내가 만든 나만의 굳이에 달린 기록)
    @Query("""
            select cm.imageUrl from CompleteMission cm
            where cm.user.id = :userId
               or cm.userMission.mission.creator.id = :userId
            """)
    List<String> findImageUrlsForWithdraw(@Param("userId") Long userId);

    // 회원 탈퇴: 내 기록 + 내가 만든 나만의 굳이에 달린 기록 삭제
    @Modifying
    @Query("""
            delete from CompleteMission cm
            where cm.user.id = :userId
               or cm.userMission.id in (
                   select um.id from UserMission um
                   where um.mission.id in (select m.id from Mission m where m.creator.id = :userId)
               )
            """)
    int deleteAllForWithdraw(@Param("userId") Long userId);

    /**
     * 해당 기간에 represent=true인 기록보다 더 나중에 만들어진 기록이 있으면 true
     */
    @Query("""
        select case when count(c) > 0 then true else false end
        from CompleteMission c
        where c.user.id = :userId
          and c.represent = true
          and c.createdAt >= :start and c.createdAt < :end
          and exists (
              select 1 from CompleteMission newer
              where newer.user = c.user
                and newer.createdAt >= :start and newer.createdAt < :end
                and newer.createdAt > c.createdAt
          )
        """)
    boolean existsNewerThanRepresent(@Param("userId") Long userId,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    /**
     * 특정 날짜의 대표 기록이 그날 가장 최신 기록이 아니면 true
     */
    default boolean isRepresentNotLatest(Long userId, LocalDate date) {
        return existsNewerThanRepresent(
                userId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        );
    }

    @Query("""
        select c
        from CompleteMission c
        where c.user.id = :userId
          and c.represent = true
          and c.createdAt >= :start and c.createdAt < :end
        """)
    Optional<CompleteMission> findRepresentBetween(@Param("userId") Long userId,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);

    /**
     * 특정 날짜의 대표 기록 조회 (없으면 Optional.empty)
     */
    default Optional<CompleteMission> findRepresentByDate(Long userId, LocalDate date) {
        return findRepresentBetween(
                userId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        );
    }

    @Query("""
        select cm
        from CompleteMission cm
        where cm.user.id = :userId
          and cm.represent = true
          and cm.createdAt >= :start
          and cm.createdAt < :end
        """)
    List<CompleteMission> findMonthlyRepresents(@Param("userId") Long userId,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    // 기간 안에서 가장 최신 기록 1개 (createdAt이 같으면 id가 큰 것)
    Optional<CompleteMission> findFirstByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDescCompleteMissionIdDesc(
            Long userId, LocalDateTime start, LocalDateTime end);

    /**
     * 특정 날짜의 가장 최신 기록 (그날 기록이 없으면 Optional.empty)
     */
    default Optional<CompleteMission> findLatestByDate(Long userId, LocalDate date) {
        return findFirstByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDescCompleteMissionIdDesc(
                userId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        );
    }
}
