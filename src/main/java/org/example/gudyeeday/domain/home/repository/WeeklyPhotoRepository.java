package org.example.gudyeeday.domain.home.repository;

import org.example.gudyeeday.domain.home.entity.WeeklyPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyPhotoRepository extends JpaRepository<WeeklyPhoto, Long> {

    Optional<WeeklyPhoto> findByUserIdAndPhotoDate(Long userId, LocalDate photoDate);

    // 기간(양 끝 포함) 내 사진
    List<WeeklyPhoto> findByUserIdAndPhotoDateBetween(Long userId, LocalDate from, LocalDate to);

    // 회원 탈퇴: 지울 주간 사진 주소
    @Query("select wp.imageUrl from WeeklyPhoto wp where wp.user.id = :userId")
    List<String> findImageUrlsByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from WeeklyPhoto wp where wp.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
