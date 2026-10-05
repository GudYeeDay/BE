package org.example.gudyeeday.domain.home.service;

import lombok.RequiredArgsConstructor;
import org.example.gudyeeday.common.exception.CustomException;
import org.example.gudyeeday.common.s3.S3Uploader;
import org.example.gudyeeday.domain.complete.entity.CompleteMission;
import org.example.gudyeeday.domain.complete.repository.CompleteMissionRepository;
import org.example.gudyeeday.domain.home.dto.response.HomeResponse;
import org.example.gudyeeday.domain.home.dto.response.PastRecordResponse;
import org.example.gudyeeday.domain.home.dto.response.WeeklyPhotoDayResponse;
import org.example.gudyeeday.domain.home.dto.response.WeeklyPhotoResponse;
import org.example.gudyeeday.domain.home.dto.response.WeeklyPhotosResponse;
import org.example.gudyeeday.domain.home.entity.WeeklyPhoto;
import org.example.gudyeeday.domain.home.exception.HomeErrorCode;
import org.example.gudyeeday.domain.home.repository.WeeklyPhotoRepository;
import org.example.gudyeeday.domain.mission.repository.UserMissionRepository;
import org.example.gudyeeday.domain.notification.repository.NotificationRepository;
import org.example.gudyeeday.domain.user.entity.User;
import org.example.gudyeeday.domain.user.exception.AuthErrorCode;
import org.example.gudyeeday.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    // 한 주는 월요일에 시작해 일요일에 끝남
    private static final DayOfWeek WEEK_START = DayOfWeek.MONDAY;
    private static final String WEEKLY_PHOTO_DIR = "weekly-photos";
    private static final int PAST_RECORD_COUNT = 5;

    private final WeeklyPhotoRepository weeklyPhotoRepository;
    private final UserMissionRepository userMissionRepository;
    private final CompleteMissionRepository completeMissionRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final S3Uploader s3Uploader;
    private final Clock clock;

    // 홈: 닉네임, 안 읽은 알림 여부, 이번 달 완료 일수, 이번 주(KST) 사진, 지난 낭만들
    public HomeResponse getHome(String email) {
        User user = getUser(email);
        LocalDate today = LocalDate.now(clock);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(WEEK_START));

        return new HomeResponse(
                user.getName(),
                notificationRepository.existsByUserIdAndReadAtIsNull(user.getId()),
                countMonthlyCompletedDays(user, today),
                getWeeklyPhotos(user, today, weekStart),
                getPastRecords(user, weekStart)
        );
    }

    // 오늘(KST) 사진 업로드, 이미 있으면 교체
    // 사용자 row에 락을 걸어 같은 사용자의 동시 업로드를 직렬화
    @Transactional
    public WeeklyPhotoResponse uploadTodayPhoto(String email, MultipartFile file) {
        validateImage(file);
        User user = userRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
        LocalDate today = LocalDate.now(clock);

        String imageUrl = s3Uploader.upload(file, WEEKLY_PHOTO_DIR);
        deleteOnRollback(imageUrl);

        WeeklyPhoto existing = weeklyPhotoRepository.findByUserIdAndPhotoDate(user.getId(), today).orElse(null);
        if (existing != null) {
            String oldImageUrl = existing.getImageUrl();
            existing.changeImage(imageUrl);
            deleteAfterCommit(oldImageUrl);
            return WeeklyPhotoResponse.from(existing);
        }

        WeeklyPhoto weeklyPhoto = weeklyPhotoRepository.save(WeeklyPhoto.createWeeklyPhoto(user, today, imageUrl));
        return WeeklyPhotoResponse.from(weeklyPhoto);
    }

    // 이번 주와 겹치지 않도록 이번 주 월요일 0시 이전에 완료한 기록 중에서 랜덤으로 고름
    private List<PastRecordResponse> getPastRecords(User user, LocalDate weekStart) {
        List<Long> ids = new ArrayList<>(
                completeMissionRepository.findIdsByUserIdAndCompletedAtBefore(user.getId(), weekStart.atStartOfDay())
        );
        if (ids.isEmpty()) {
            return List.of();
        }

        Collections.shuffle(ids);
        List<Long> pickedIds = ids.subList(0, Math.min(PAST_RECORD_COUNT, ids.size()));

        // IN 조회는 순서를 보장하지 않으므로 뽑은 순서대로 다시 정렬
        Map<Long, CompleteMission> recordsById = completeMissionRepository.findWithMissionByIdIn(pickedIds).stream()
                .collect(Collectors.toMap(CompleteMission::getCompleteMissionId, Function.identity()));

        return pickedIds.stream()
                .map(recordsById::get)
                .filter(Objects::nonNull)
                .map(PastRecordResponse::from)
                .toList();
    }

    private WeeklyPhotosResponse getWeeklyPhotos(User user, LocalDate today, LocalDate startDate) {
        LocalDate endDate = startDate.plusDays(DayOfWeek.values().length - 1);

        Map<LocalDate, WeeklyPhoto> photosByDate = weeklyPhotoRepository
                .findByUserIdAndPhotoDateBetween(user.getId(), startDate, endDate).stream()
                .collect(Collectors.toMap(WeeklyPhoto::getPhotoDate, Function.identity()));

        List<WeeklyPhotoDayResponse> days = startDate.datesUntil(endDate.plusDays(1))
                .map(date -> WeeklyPhotoDayResponse.of(date, today, photosByDate.get(date)))
                .toList();

        return new WeeklyPhotosResponse(startDate, endDate, days);
    }

    // 이번 달 완료 기록을 날짜 단위로 중복 제거해 셈
    private int countMonthlyCompletedDays(User user, LocalDate today) {
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime nextMonthStart = monthStart.plusMonths(1);

        return (int) userMissionRepository.findCompletedAtBetween(user.getId(), monthStart, nextMonthStart).stream()
                .map(LocalDateTime::toLocalDate)
                .distinct()
                .count();
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(HomeErrorCode.WEEKLY_PHOTO_EMPTY);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CustomException(HomeErrorCode.WEEKLY_PHOTO_NOT_IMAGE);
        }
    }

    // DB 반영이 실패하면 방금 올린 파일을 지움
    private void deleteOnRollback(String imageUrl) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    s3Uploader.delete(imageUrl);
                }
            }
        });
    }

    // 교체 전 파일은 DB 반영이 끝난 뒤 지움
    private void deleteAfterCommit(String imageUrl) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            s3Uploader.delete(imageUrl);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                s3Uploader.delete(imageUrl);
            }
        });
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
    }
}
