package org.example.gudyeeday.domain.home.service;

import org.example.gudyeeday.common.exception.CustomException;
import org.example.gudyeeday.common.s3.S3Uploader;
import org.example.gudyeeday.domain.complete.entity.CompleteMission;
import org.example.gudyeeday.domain.complete.repository.CompleteMissionRepository;
import org.example.gudyeeday.domain.home.dto.response.HomeResponse;
import org.example.gudyeeday.domain.home.dto.response.PastRecordResponse;
import org.example.gudyeeday.domain.home.dto.response.WeeklyPhotoDayResponse;
import org.example.gudyeeday.domain.home.dto.response.WeeklyPhotoResponse;
import org.example.gudyeeday.domain.home.entity.WeeklyPhoto;
import org.example.gudyeeday.domain.home.exception.HomeErrorCode;
import org.example.gudyeeday.domain.home.repository.WeeklyPhotoRepository;
import org.example.gudyeeday.domain.mission.entity.Mission;
import org.example.gudyeeday.domain.mission.entity.UserMission;
import org.example.gudyeeday.domain.mission.enums.DayType;
import org.example.gudyeeday.domain.mission.enums.Season;
import org.example.gudyeeday.domain.mission.repository.UserMissionRepository;
import org.example.gudyeeday.domain.notification.repository.NotificationRepository;
import org.example.gudyeeday.domain.user.entity.User;
import org.example.gudyeeday.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class HomeServiceTest {

    private static final String EMAIL = "user@test.com";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String IMAGE_URL = "https://bucket.s3.ap-northeast-2.amazonaws.com/weekly-photos/new.jpg";

    private final WeeklyPhotoRepository weeklyPhotoRepository = mock(WeeklyPhotoRepository.class);
    private final UserMissionRepository userMissionRepository = mock(UserMissionRepository.class);
    private final CompleteMissionRepository completeMissionRepository = mock(CompleteMissionRepository.class);
    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final S3Uploader s3Uploader = mock(S3Uploader.class);

    private User user;

    @BeforeEach
    void setUp() {
        user = User.createSocialUser(EMAIL, "Pawket", "google-1");
        ReflectionTestUtils.setField(user, "id", 1L);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(user));
        when(s3Uploader.upload(any(), any())).thenReturn(IMAGE_URL);
    }

    @Test
    void 홈은_닉네임과_오늘이_속한_월요일부터_일요일까지_7일을_반환한다() {
        // 2026-10-03(토)
        HomeService homeService = homeService(LocalDateTime.of(2026, 10, 3, 10, 0));
        LocalDate monday = LocalDate.of(2026, 9, 28);
        LocalDate sunday = LocalDate.of(2026, 10, 4);
        WeeklyPhoto tuesdayPhoto = weeklyPhoto(5L, monday.plusDays(1), "tue.jpg");
        when(weeklyPhotoRepository.findByUserIdAndPhotoDateBetween(1L, monday, sunday)).thenReturn(List.of(tuesdayPhoto));

        HomeResponse result = homeService.getHome(EMAIL);

        assertThat(result.name()).isEqualTo("Pawket");
        assertThat(result.hasUnreadNotification()).isFalse();
        assertThat(result.weeklyPhotos().startDate()).isEqualTo(monday);
        assertThat(result.weeklyPhotos().endDate()).isEqualTo(sunday);
        assertThat(result.weeklyPhotos().days()).extracting(WeeklyPhotoDayResponse::dayOfWeek)
                .containsExactly(DayOfWeek.values());
        assertThat(result.weeklyPhotos().days().get(1).imageUrl()).isEqualTo("tue.jpg");
        assertThat(result.weeklyPhotos().days().get(0).imageUrl()).isNull();
        assertThat(result.weeklyPhotos().days()).filteredOn(WeeklyPhotoDayResponse::today)
                .extracting(WeeklyPhotoDayResponse::date)
                .containsExactly(LocalDate.of(2026, 10, 3));
    }

    @Test
    void 안_읽은_알림이_있으면_표시한다() {
        when(notificationRepository.existsByUserIdAndReadAtIsNull(1L)).thenReturn(true);

        assertThat(homeService(LocalDateTime.of(2026, 10, 3, 10, 0)).getHome(EMAIL).hasUnreadNotification()).isTrue();
    }

    @Test
    void 일요일까지는_같은_주이고_월요일이_되면_새_주가_시작된다() {
        homeService(LocalDateTime.of(2026, 10, 4, 23, 59)).getHome(EMAIL);
        verify(weeklyPhotoRepository).findByUserIdAndPhotoDateBetween(1L, LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 4));

        homeService(LocalDateTime.of(2026, 10, 5, 0, 0)).getHome(EMAIL);
        verify(weeklyPhotoRepository).findByUserIdAndPhotoDateBetween(1L, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 11));
    }

    @Test
    void 이번_달_완료_일수는_하루에_여러_번_완료해도_1일로_센다() {
        HomeService homeService = homeService(LocalDateTime.of(2026, 10, 3, 10, 0));
        when(userMissionRepository.findCompletedAtBetween(1L, LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 11, 1, 0, 0)))
                .thenReturn(List.of(
                        LocalDateTime.of(2026, 10, 1, 9, 0),
                        LocalDateTime.of(2026, 10, 1, 21, 0),
                        LocalDateTime.of(2026, 10, 3, 8, 0)
                ));

        assertThat(homeService.getHome(EMAIL).monthlyCompletedDays()).isEqualTo(2);
    }

    @Test
    void 오늘_사진이_없으면_오늘_날짜로_새로_저장한다() {
        HomeService homeService = homeService(LocalDateTime.of(2026, 10, 3, 10, 0));
        LocalDate today = LocalDate.of(2026, 10, 3);
        when(weeklyPhotoRepository.findByUserIdAndPhotoDate(1L, today)).thenReturn(Optional.empty());
        when(weeklyPhotoRepository.save(any(WeeklyPhoto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WeeklyPhotoResponse result = homeService.uploadTodayPhoto(EMAIL, image());

        assertThat(result.date()).isEqualTo(today);
        assertThat(result.imageUrl()).isEqualTo(IMAGE_URL);
        verify(s3Uploader, never()).delete(any());
    }

    @Test
    void 오늘_사진이_이미_있으면_교체하고_이전_파일을_지운다() {
        HomeService homeService = homeService(LocalDateTime.of(2026, 10, 3, 10, 0));
        LocalDate today = LocalDate.of(2026, 10, 3);
        WeeklyPhoto existing = weeklyPhoto(5L, today, "old.jpg");
        when(weeklyPhotoRepository.findByUserIdAndPhotoDate(1L, today)).thenReturn(Optional.of(existing));

        WeeklyPhotoResponse result = homeService.uploadTodayPhoto(EMAIL, image());

        assertThat(result.weeklyPhotoId()).isEqualTo(5L);
        assertThat(existing.getImageUrl()).isEqualTo(IMAGE_URL);
        verify(weeklyPhotoRepository, never()).save(any());
        verify(s3Uploader).delete("old.jpg");
    }

    @Test
    void 이미지가_아니면_업로드하지_않는다() {
        HomeService homeService = homeService(LocalDateTime.of(2026, 10, 3, 10, 0));
        MockMultipartFile text = new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> homeService.uploadTodayPhoto(EMAIL, text))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(HomeErrorCode.WEEKLY_PHOTO_NOT_IMAGE);
        verify(s3Uploader, never()).upload(any(), any());
    }

    @Test
    void 빈_파일은_업로드하지_않는다() {
        HomeService homeService = homeService(LocalDateTime.of(2026, 10, 3, 10, 0));
        MockMultipartFile empty = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> homeService.uploadTodayPhoto(EMAIL, empty))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(HomeErrorCode.WEEKLY_PHOTO_EMPTY);
    }

    @Test
    void 지난_낭만들은_이번_주_월요일_0시_이전_기록에서_최대_5개를_중복_없이_반환한다() {
        // 2026-10-03(토) -> 이번 주 월요일 2026-09-28
        HomeService homeService = homeService(LocalDateTime.of(2026, 10, 3, 10, 0));
        List<Long> ids = LongStream.rangeClosed(1, 8).boxed().toList();
        when(completeMissionRepository.findIdsByUserIdAndCompletedAtBefore(1L, LocalDateTime.of(2026, 9, 28, 0, 0)))
                .thenReturn(ids);
        when(completeMissionRepository.findWithMissionByIdIn(anyCollection())).thenAnswer(invocation -> {
            Collection<Long> picked = invocation.getArgument(0);
            return picked.stream().map(id -> completeMission(id, LocalDateTime.of(2026, 9, 1, 9, 0))).toList();
        });

        List<PastRecordResponse> result = homeService.getHome(EMAIL).pastRecords();

        assertThat(result).hasSize(5);
        assertThat(result).extracting(PastRecordResponse::completeMissionId).doesNotHaveDuplicates().isSubsetOf(ids);
    }

    @Test
    void 지난_낭만들은_미션_이름_설명_완료_날짜_위치를_반환한다() {
        HomeService homeService = homeService(LocalDateTime.of(2026, 10, 3, 10, 0));
        when(completeMissionRepository.findIdsByUserIdAndCompletedAtBefore(any(), any())).thenReturn(List.of(7L));
        when(completeMissionRepository.findWithMissionByIdIn(List.of(7L)))
                .thenReturn(List.of(completeMission(7L, LocalDateTime.of(2026, 9, 14, 18, 30))));

        List<PastRecordResponse> result = homeService.getHome(EMAIL).pastRecords();

        assertThat(result).containsExactly(new PastRecordResponse(
                7L, "record-7.jpg", "퇴근길 한 정거장 걸어보기", "한 정거장 일찍 내려서 걸어보기",
                LocalDate.of(2026, 9, 14), "퇴근길 버스정류장"));
    }

    @Test
    void 지난_기록이_없으면_빈_목록을_반환한다() {
        HomeService homeService = homeService(LocalDateTime.of(2026, 10, 3, 10, 0));
        when(completeMissionRepository.findIdsByUserIdAndCompletedAtBefore(any(), any())).thenReturn(List.of());

        assertThat(homeService.getHome(EMAIL).pastRecords()).isEmpty();
        verify(completeMissionRepository, never()).findWithMissionByIdIn(any());
    }

    private HomeService homeService(LocalDateTime now) {
        Clock clock = Clock.fixed(now.atZone(KST).toInstant(), KST);
        return new HomeService(weeklyPhotoRepository, userMissionRepository, completeMissionRepository, notificationRepository, userRepository, s3Uploader, clock);
    }

    private CompleteMission completeMission(Long id, LocalDateTime completedAt) {
        Mission mission = Mission.createMission("퇴근길 한 정거장 걸어보기", "한 정거장 일찍 내려서 걸어보기", DayType.ALL, Season.ALL);
        UserMission userMission = UserMission.startMission(user, mission);
        userMission.complete(completedAt);
        CompleteMission completeMission = CompleteMission.create(user, userMission, "record-" + id + ".jpg", "퇴근길 버스정류장", "기록 내용");
        ReflectionTestUtils.setField(completeMission, "completeMissionId", id);
        return completeMission;
    }

    private WeeklyPhoto weeklyPhoto(Long id, LocalDate date, String imageUrl) {
        WeeklyPhoto weeklyPhoto = WeeklyPhoto.createWeeklyPhoto(user, date, imageUrl);
        ReflectionTestUtils.setField(weeklyPhoto, "id", id);
        return weeklyPhoto;
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }
}
