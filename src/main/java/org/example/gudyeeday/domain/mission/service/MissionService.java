package org.example.gudyeeday.domain.mission.service;

import lombok.RequiredArgsConstructor;
import org.example.gudyeeday.common.exception.CustomException;
import org.example.gudyeeday.domain.mission.dto.response.InProgressMissionResponse;
import org.example.gudyeeday.domain.mission.dto.response.MissionBookmarkResponse;
import org.example.gudyeeday.domain.mission.dto.response.MissionRecommendResponse;
import org.example.gudyeeday.domain.mission.entity.Mission;
import org.example.gudyeeday.domain.mission.entity.MissionBookmark;
import org.example.gudyeeday.domain.mission.entity.UserMission;
import org.example.gudyeeday.domain.mission.enums.DayType;
import org.example.gudyeeday.domain.mission.enums.Season;
import org.example.gudyeeday.domain.mission.enums.UserMissionStatus;
import org.example.gudyeeday.domain.mission.exception.MissionErrorCode;
import org.example.gudyeeday.domain.mission.repository.MissionBookmarkRepository;
import org.example.gudyeeday.domain.mission.repository.MissionRepository;
import org.example.gudyeeday.domain.mission.repository.UserMissionRepository;
import org.example.gudyeeday.domain.user.entity.User;
import org.example.gudyeeday.domain.user.exception.AuthErrorCode;
import org.example.gudyeeday.domain.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissionService {

    private static final int RECOMMEND_COUNT = 3;

    private final MissionRepository missionRepository;
    private final MissionBookmarkRepository missionBookmarkRepository;
    private final UserMissionRepository userMissionRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    // 오늘(KST) 요일/계절에 맞는 미션 중 보관함에 없고 진행중이 아닌 미션을 랜덤으로 3개 추천
    public List<MissionRecommendResponse> recommendMissions(String email) {
        User user = getUser(email);
        LocalDate today = LocalDate.now(clock);

        List<Mission> candidates = missionRepository.findRecommendCandidates(
                user.getId(),
                List.of(DayType.ALL, DayType.from(today.getDayOfWeek())),
                List.of(Season.ALL, Season.from(today.getMonth()))
        );

        Collections.shuffle(candidates);

        return candidates.stream()
                .limit(RECOMMEND_COUNT)
                .map(MissionRecommendResponse::from)
                .toList();
    }

    // 추천 미션을 보관함에 저장
    @Transactional
    public MissionBookmarkResponse bookmarkMission(String email, Long missionId) {
        User user = getUser(email);
        Mission mission = getMission(missionId);

        if (missionBookmarkRepository.existsByUserIdAndMissionId(user.getId(), mission.getId())) {
            throw new CustomException(MissionErrorCode.ALREADY_BOOKMARKED);
        }

        MissionBookmark bookmark;
        try {
            bookmark = missionBookmarkRepository.save(
                    MissionBookmark.createMissionBookmark(user, mission)
            );
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 그 사이에 같은 미션이 저장된 경우
            throw new CustomException(MissionErrorCode.ALREADY_BOOKMARKED);
        }

        return MissionBookmarkResponse.from(bookmark);
    }

    // 보관함 저장 해제
    @Transactional
    public void unbookmarkMission(String email, Long missionId) {
        User user = getUser(email);

        MissionBookmark bookmark = missionBookmarkRepository.findByUserIdAndMissionId(user.getId(), missionId)
                .orElseThrow(() -> new CustomException(MissionErrorCode.BOOKMARK_NOT_FOUND));

        missionBookmarkRepository.delete(bookmark);
    }

    // 미션 시작 (한 번에 하나만 진행 가능)
    // 사용자 row에 락을 걸어, 같은 사용자의 동시 시작 요청은 먼저 들어온 요청이 끝날 때까지 대기 후 진행중 여부를 다시 확인
    @Transactional
    public InProgressMissionResponse startMission(String email, Long missionId) {
        User user = userRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
        Mission mission = getMission(missionId);

        if (userMissionRepository.existsByUserIdAndStatus(user.getId(), UserMissionStatus.IN_PROGRESS)) {
            throw new CustomException(MissionErrorCode.MISSION_ALREADY_IN_PROGRESS);
        }

        UserMission userMission = userMissionRepository.save(UserMission.startMission(user, mission));

        return InProgressMissionResponse.from(userMission);
    }

    // 진행중인 미션 상세 조회
    public InProgressMissionResponse getInProgressMission(String email) {
        User user = getUser(email);
        return InProgressMissionResponse.from(getInProgressUserMission(user));
    }

    // 진행중인 미션 완료
    @Transactional
    public InProgressMissionResponse completeInProgressMission(String email) {
        User user = getUser(email);
        UserMission userMission = getInProgressUserMission(user);

        userMission.complete(LocalDateTime.now(clock));

        return InProgressMissionResponse.from(userMission);
    }

    // 진행중인 미션 그만두기 (진행 기록 삭제)
    @Transactional
    public void quitInProgressMission(String email) {
        User user = getUser(email);
        userMissionRepository.delete(getInProgressUserMission(user));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
    }

    private Mission getMission(Long missionId) {
        return missionRepository.findById(missionId)
                .orElseThrow(() -> new CustomException(MissionErrorCode.MISSION_NOT_FOUND));
    }

    private UserMission getInProgressUserMission(User user) {
        return userMissionRepository.findByUserIdAndStatus(user.getId(), UserMissionStatus.IN_PROGRESS)
                .orElseThrow(() -> new CustomException(MissionErrorCode.IN_PROGRESS_MISSION_NOT_FOUND));
    }
}
