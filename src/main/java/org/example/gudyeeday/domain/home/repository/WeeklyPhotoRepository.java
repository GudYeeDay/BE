package org.example.gudyeeday.domain.home.repository;

import org.example.gudyeeday.domain.home.entity.WeeklyPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyPhotoRepository extends JpaRepository<WeeklyPhoto, Long> {

    Optional<WeeklyPhoto> findByUserIdAndPhotoDate(Long userId, LocalDate photoDate);

    // 기간(양 끝 포함) 내 사진
    List<WeeklyPhoto> findByUserIdAndPhotoDateBetween(Long userId, LocalDate from, LocalDate to);
}
