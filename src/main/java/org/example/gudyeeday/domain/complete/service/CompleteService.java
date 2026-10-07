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
    public CompleteMissionResponse completeMission(String email, Long userMissionId, MultipartFile image, CompleteMissionRequest request) {
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

        String imageUrl = s3Uploader.upload(image, "complete-mission");

        userMission.complete(LocalDateTime.now());

        CompleteMission completeMission = CompleteMission.create(
                user,
                userMission,
                imageUrl,
                request.location(),
                request.content()
        );

        CompleteMission newCompleteMission = completeMissionRepository.save(completeMission);

        return CompleteMissionResponse.from(newCompleteMission);

    }

    @Transactional
    public List<CompleteMissionResponse> dailyFeed(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);

        return completeMissionRepository
                .findTodayCompleteMissions(user, startOfToday, startOfTomorrow)
                .stream()
                .map(CompleteMissionResponse::from)
                .toList();

    }

    @Transactional
    public CompleteMissionResponse updateRecord(String email, Long completeMissionId, MultipartFile image, CompleteMissionRequest request) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        CompleteMission completeMission = completeMissionRepository.findById(completeMissionId).orElseThrow(() -> new CustomException(MissionErrorCode.MISSION_NOT_FOUND));

        if (!completeMission.getUser().getId().equals(user.getId())) {
            throw new CustomException(CompleteErrorCode.MISSION_ACCESS_DENIED);
        }

        if (image != null && !image.isEmpty()) {
            String oldImageUrl = completeMission.getImageUrl();
            String newImageUrl = s3Uploader.upload(image, "complete-mission");
            completeMission.updateImage(newImageUrl);
            s3Uploader.delete(oldImageUrl);
        }

        completeMission.updateRecord(request.location(), request.content());

        return CompleteMissionResponse.from(completeMission);

    }

    @Transactional
    public Long deleteRecord(String email, Long completeMissionId) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        CompleteMission completeMission = completeMissionRepository.findById(completeMissionId).orElseThrow(() -> new CustomException(MissionErrorCode.MISSION_NOT_FOUND));

        if (!completeMission.getUser().getId().equals(user.getId())) {
            throw new CustomException(CompleteErrorCode.MISSION_ACCESS_DENIED);
        }

        Long deleteCompleteId = completeMission.getCompleteMissionId();

        s3Uploader.delete(completeMission.getImageUrl());

        completeMissionRepository.delete(completeMission);

        return deleteCompleteId;

    }



}
