package org.example.gudyeeday.domain.complete.service;

import org.example.gudyeeday.common.s3.S3Uploader;
import org.example.gudyeeday.domain.complete.dto.request.CompleteMissionRequest;
import org.example.gudyeeday.domain.complete.entity.CompleteMission;
import org.example.gudyeeday.domain.complete.repository.CompleteMissionRepository;
import org.example.gudyeeday.domain.mission.entity.Mission;
import org.example.gudyeeday.domain.mission.entity.MissionBookmark;
import org.example.gudyeeday.domain.mission.entity.UserMission;
import org.example.gudyeeday.domain.mission.enums.DayType;
import org.example.gudyeeday.domain.mission.enums.Season;
import org.example.gudyeeday.domain.mission.enums.UserMissionStatus;
import org.example.gudyeeday.domain.mission.repository.MissionBookmarkRepository;
import org.example.gudyeeday.domain.mission.repository.UserMissionRepository;
import org.example.gudyeeday.domain.user.entity.User;
import org.example.gudyeeday.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CompleteServiceTest {

    private static final String EMAIL = "user@test.com";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserMissionRepository userMissionRepository = mock(UserMissionRepository.class);
    private final CompleteMissionRepository completeMissionRepository = mock(CompleteMissionRepository.class);
    private final MissionBookmarkRepository missionBookmarkRepository = mock(MissionBookmarkRepository.class);
    private final S3Uploader s3Uploader = mock(S3Uploader.class);

    private CompleteService completeService;
    private User user;
    private Mission mission;
    private UserMission userMission;

    @BeforeEach
    void setUp() {
        completeService = new CompleteService(userRepository, userMissionRepository, completeMissionRepository, missionBookmarkRepository, s3Uploader);

        user = User.createSocialUser(EMAIL, "사용자", "google-1");
        ReflectionTestUtils.setField(user, "id", 1L);
        mission = Mission.createMission("미션", "설명", DayType.ALL, Season.ALL);
        ReflectionTestUtils.setField(mission, "id", 10L);
        userMission = UserMission.startMission(user, mission);
        ReflectionTestUtils.setField(userMission, "id", 100L);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userMissionRepository.findById(100L)).thenReturn(Optional.of(userMission));
        when(s3Uploader.upload(any(), any())).thenReturn("record.jpg");
        when(completeMissionRepository.save(any(CompleteMission.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void 보관함에_없던_미션을_기록으로_완료하면_보관함에_저장된다() {
        when(missionBookmarkRepository.findByUserIdAndMissionId(1L, 10L)).thenReturn(Optional.empty());

        completeService.completeMission(EMAIL, 100L, image(), new CompleteMissionRequest("위치", "내용"));

        assertThat(userMission.getStatus()).isEqualTo(UserMissionStatus.COMPLETED);
        verify(missionBookmarkRepository).save(argThat(b ->
                b.getUser() == user && b.getMission() == mission && b.getSavedAt().equals(userMission.getCompletedAt())));
    }

    @Test
    void 이미_보관함에_있는_미션은_다시_저장하지_않는다() {
        MissionBookmark existing = MissionBookmark.createMissionBookmark(user, mission, LocalDateTime.of(2026, 9, 1, 9, 0));
        when(missionBookmarkRepository.findByUserIdAndMissionId(1L, 10L)).thenReturn(Optional.of(existing));

        completeService.completeMission(EMAIL, 100L, image(), new CompleteMissionRequest("위치", "내용"));

        verify(missionBookmarkRepository, never()).save(any());
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("image", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }
}
