package org.example.gudyeeday.domain.user.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gudyeeday.common.s3.S3Uploader;
import org.example.gudyeeday.domain.complete.repository.CompleteMissionRepository;
import org.example.gudyeeday.domain.home.repository.WeeklyPhotoRepository;
import org.example.gudyeeday.domain.mission.repository.MissionBookmarkRepository;
import org.example.gudyeeday.domain.mission.repository.MissionRepository;
import org.example.gudyeeday.domain.mission.repository.UserMissionRepository;
import org.example.gudyeeday.domain.notification.repository.DeviceTokenRepository;
import org.example.gudyeeday.domain.notification.repository.NotificationRepository;
import org.example.gudyeeday.domain.user.repository.EmailVerificationRepository;
import org.example.gudyeeday.domain.user.repository.RefreshTokenRepository;
import org.example.gudyeeday.domain.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// 회원 탈퇴 시 사용자와 연결된 데이터를 모두 삭제
// 외래키 때문에 자식 테이블부터 지움:
//   complete_mission -> weekly_photo -> user_mission -> mission_bookmark -> mission(나만의 굳이)
//   -> notification -> device_token -> refresh_token -> email_verification -> user
// S3 사진은 DB 삭제가 커밋된 뒤에 지움 (롤백되면 사진은 남겨야 하므로)
@Slf4j
@Component
@RequiredArgsConstructor
public class UserDataDeleter {

    private final CompleteMissionRepository completeMissionRepository;
    private final WeeklyPhotoRepository weeklyPhotoRepository;
    private final UserMissionRepository userMissionRepository;
    private final MissionBookmarkRepository missionBookmarkRepository;
    private final MissionRepository missionRepository;
    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final S3Uploader s3Uploader;

    @Transactional
    public void deleteAll(Long userId, String email) {
        // 지우기 전에 S3 사진 주소 모으기 (기록 사진과 주간 사진은 같은 주소일 수 있어서 중복 제거)
        Set<String> imageUrls = new LinkedHashSet<>();
        imageUrls.addAll(completeMissionRepository.findImageUrlsForWithdraw(userId));
        imageUrls.addAll(weeklyPhotoRepository.findImageUrlsByUserId(userId));

        int completeMissions = completeMissionRepository.deleteAllForWithdraw(userId);
        int weeklyPhotos = weeklyPhotoRepository.deleteAllByUserId(userId);
        int userMissions = userMissionRepository.deleteAllForWithdraw(userId);
        int bookmarks = missionBookmarkRepository.deleteAllForWithdraw(userId);
        int customMissions = missionRepository.deleteAllByCreatorId(userId);
        int notifications = notificationRepository.deleteAllByUserId(userId);
        int deviceTokens = deviceTokenRepository.deleteAllByUserId(userId);
        int refreshTokens = refreshTokenRepository.deleteAllByUserId(userId);
        emailVerificationRepository.deleteByEmail(email);
        userRepository.deleteById(userId);

        log.info("탈퇴 데이터 삭제: userId={}, 완료기록={}, 주간사진={}, 진행기록={}, 보관함={}, 나만의굳이={}, 알림={}, 기기토큰={}, 로그인토큰={}, S3사진={}",
                userId, completeMissions, weeklyPhotos, userMissions, bookmarks, customMissions,
                notifications, deviceTokens, refreshTokens, imageUrls.size());

        eventPublisher.publishEvent(new UserImagesDeleteEvent(new ArrayList<>(imageUrls)));
    }

    // DB 삭제가 커밋된 뒤 S3 사진 삭제 (실패해도 탈퇴는 이미 완료, S3Uploader가 로그만 남김)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deleteImages(UserImagesDeleteEvent event) {
        event.imageUrls().forEach(s3Uploader::delete);
    }

    public record UserImagesDeleteEvent(List<String> imageUrls) {
    }
}