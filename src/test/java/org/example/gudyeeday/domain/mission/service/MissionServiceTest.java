package org.example.gudyeeday.domain.mission.service;

import org.example.gudyeeday.common.exception.CustomException;
import org.example.gudyeeday.domain.mission.dto.response.InProgressMissionResponse;
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
import org.example.gudyeeday.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MissionServiceTest {

    private static final String EMAIL = "user@test.com";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    // 2026-09-25(금) 10:00 KST -> 평일, 가을
    private static final ZonedDateTime NOW = ZonedDateTime.of(2026, 9, 25, 10, 0, 0, 0, KST);

    private final MissionRepository missionRepository = mock(MissionRepository.class);
    private final MissionBookmarkRepository missionBookmarkRepository = mock(MissionBookmarkRepository.class);
    private final UserMissionRepository userMissionRepository = mock(UserMissionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final Clock clock = Clock.fixed(NOW.toInstant(), KST);

    private MissionService missionService;
    private User user;
    private Mission mission;

    @BeforeEach
    void setUp() {
        missionService = new MissionService(missionRepository, missionBookmarkRepository, userMissionRepository, userRepository, clock);

        user = User.createSocialUser(EMAIL, "사용자", "google-1");
        ReflectionTestUtils.setField(user, "id", 1L);
        mission = mission(10L);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(user));
        when(missionRepository.findById(10L)).thenReturn(Optional.of(mission));
    }

    @Test
    void 추천은_오늘의_요일_계절로_조회하고_최대_3개를_반환한다() {
        List<Mission> candidates = new ArrayList<>(IntStream.rangeClosed(1, 5).mapToObj(i -> mission((long) i)).toList());
        when(missionRepository.findRecommendCandidates(1L, List.of(DayType.ALL, DayType.WEEKDAY), List.of(Season.ALL, Season.FALL)))
                .thenReturn(candidates);

        List<MissionRecommendResponse> result = missionService.recommendMissions(EMAIL);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(MissionRecommendResponse::missionId).doesNotHaveDuplicates();
    }

    @Test
    void 추천_후보가_3개보다_적으면_있는_만큼만_반환한다() {
        when(missionRepository.findRecommendCandidates(any(), any(), any()))
                .thenReturn(new ArrayList<>(List.of(mission(1L))));

        assertThat(missionService.recommendMissions(EMAIL)).hasSize(1);
    }

    @Test
    void 이미_저장한_미션을_저장하면_ALREADY_BOOKMARKED() {
        when(missionBookmarkRepository.existsByUserIdAndMissionId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> missionService.bookmarkMission(EMAIL, 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(MissionErrorCode.ALREADY_BOOKMARKED);
        verify(missionBookmarkRepository, never()).save(any());
    }

    @Test
    void 동시_저장으로_유니크_제약에_걸려도_ALREADY_BOOKMARKED() {
        when(missionBookmarkRepository.existsByUserIdAndMissionId(1L, 10L)).thenReturn(false);
        when(missionBookmarkRepository.save(any(MissionBookmark.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        assertThatThrownBy(() -> missionService.bookmarkMission(EMAIL, 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(MissionErrorCode.ALREADY_BOOKMARKED);
    }

    @Test
    void 저장하지_않은_미션을_해제하면_BOOKMARK_NOT_FOUND() {
        when(missionBookmarkRepository.findByUserIdAndMissionId(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> missionService.unbookmarkMission(EMAIL, 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(MissionErrorCode.BOOKMARK_NOT_FOUND);
    }

    @Test
    void 미션을_시작하면_진행중으로_저장된다() {
        when(userMissionRepository.existsByUserIdAndStatus(1L, UserMissionStatus.IN_PROGRESS)).thenReturn(false);
        when(userMissionRepository.save(any(UserMission.class))).thenAnswer(inv -> inv.getArgument(0));

        InProgressMissionResponse result = missionService.startMission(EMAIL, 10L);

        assertThat(result.missionId()).isEqualTo(10L);
        verify(userRepository).findByEmailForUpdate(EMAIL);
        verify(userMissionRepository).save(argThat(um -> um.getStatus() == UserMissionStatus.IN_PROGRESS));
    }

    @Test
    void 진행중인_미션이_있으면_시작할_수_없다() {
        when(userMissionRepository.existsByUserIdAndStatus(1L, UserMissionStatus.IN_PROGRESS)).thenReturn(true);

        assertThatThrownBy(() -> missionService.startMission(EMAIL, 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(MissionErrorCode.MISSION_ALREADY_IN_PROGRESS);
        verify(userMissionRepository, never()).save(any());
    }

    @Test
    void 존재하지_않는_미션은_시작할_수_없다() {
        when(missionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> missionService.startMission(EMAIL, 99L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(MissionErrorCode.MISSION_NOT_FOUND);
    }

    @Test
    void 미션을_완료하면_상태와_완료시각이_기록된다() {
        UserMission userMission = UserMission.startMission(user, mission);
        when(userMissionRepository.findByUserIdAndStatus(1L, UserMissionStatus.IN_PROGRESS)).thenReturn(Optional.of(userMission));

        missionService.completeInProgressMission(EMAIL);

        assertThat(userMission.getStatus()).isEqualTo(UserMissionStatus.COMPLETED);
        assertThat(userMission.getCompletedAt()).isEqualTo(LocalDateTime.of(2026, 9, 25, 10, 0));
    }

    @Test
    void 진행중인_미션이_없으면_완료할_수_없다() {
        when(userMissionRepository.findByUserIdAndStatus(1L, UserMissionStatus.IN_PROGRESS)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> missionService.completeInProgressMission(EMAIL))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(MissionErrorCode.IN_PROGRESS_MISSION_NOT_FOUND);
    }

    @Test
    void 그만두면_진행_기록이_삭제된다() {
        UserMission userMission = UserMission.startMission(user, mission);
        when(userMissionRepository.findByUserIdAndStatus(1L, UserMissionStatus.IN_PROGRESS)).thenReturn(Optional.of(userMission));

        missionService.quitInProgressMission(EMAIL);

        verify(userMissionRepository).delete(userMission);
    }

    private Mission mission(Long id) {
        Mission m = Mission.createMission("미션" + id, "설명", DayType.ALL, Season.ALL);
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }
}
