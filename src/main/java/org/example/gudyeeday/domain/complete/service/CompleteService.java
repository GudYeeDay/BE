package org.example.gudyeeday.domain.complete.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gudyeeday.common.exception.CustomException;
import org.example.gudyeeday.common.s3.S3Uploader;
import org.example.gudyeeday.domain.complete.dto.request.CompleteMissionRequest;
import org.example.gudyeeday.domain.complete.dto.response.CompleteMissionResponse;
import org.example.gudyeeday.domain.complete.entity.CompleteMission;
import org.example.gudyeeday.domain.complete.exception.CompleteErrorCode;
import org.example.gudyeeday.domain.complete.repository.CompleteMissionRepository;
import org.example.gudyeeday.domain.mission.entity.UserMission;
import org.example.gudyeeday.domain.mission.exception.MissionErrorCode;
import org.example.gudyeeday.domain.mission.repository.UserMissionRepository;
import org.example.gudyeeday.domain.mypage.dto.response.ProfileResponse;
import org.example.gudyeeday.domain.user.entity.User;
import org.example.gudyeeday.domain.user.exception.AuthErrorCode;
import org.example.gudyeeday.domain.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompleteService {

    private final UserRepository userRepository;
    private final UserMissionRepository userMissionRepository;
    private final CompleteMissionRepository completeMissionRepository;
    private final S3Uploader s3Uploader;

    @Transactional
    public List<CompleteMissionResponse> completeMission(String email, Long userMissionId, MultipartFile image, CompleteMissionRequest request) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        UserMission userMission = userMissionRepository.findById(userMissionId).orElseThrow(() -> new CustomException(MissionErrorCode.MISSION_NOT_FOUND));

        if (!userMission.getUser().getId().equals(user.getId())) {
            throw new CustomException(CompleteErrorCode.MISSION_ACCESS_DENIED);
        }

        if(completeMissionRepository.existsCompleteMissionByUserMission(userMission)) {
            throw new CustomException(CompleteErrorCode.ALREADY_EXIST_MISSION_RECORD);
        }

        if (image == null || image.isEmpty()) {
            throw new CustomException(CompleteErrorCode.IMAGE_REQUIRED);
        }

        // 검증이 모두 끝난 뒤 업로드
        String imageUrl = s3Uploader.upload(image, "complete-mission");

        // 미션 완료 처리 (completedAt 세팅)
        userMission.complete(LocalDateTime.now());

        CompleteMission completeMission = CompleteMission.create(
                user,
                userMission,
                imageUrl,
                request.location(),
                request.content()
        );

        completeMissionRepository.save(completeMission);

        // 오늘 완료한 기록 전체 조회
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);

        log.info("1차로 확인 request = {}", request);

        return completeMissionRepository
                .findTodayCompleteMissions(user, startOfToday, startOfTomorrow)
                .stream()
                .map(CompleteMissionResponse::from)
                .toList();

    }

}
