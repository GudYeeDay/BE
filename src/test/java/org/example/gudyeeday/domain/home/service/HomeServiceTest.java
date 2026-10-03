package org.example.gudyeeday.domain.home.service;

import org.example.gudyeeday.common.exception.CustomException;
import org.example.gudyeeday.common.s3.S3Uploader;
import org.example.gudyeeday.domain.home.dto.response.HomeResponse;
import org.example.gudyeeday.domain.home.dto.response.WeeklyPhotoDayResponse;
import org.example.gudyeeday.domain.home.dto.response.WeeklyPhotoResponse;
import org.example.gudyeeday.domain.home.entity.WeeklyPhoto;
import org.example.gudyeeday.domain.home.exception.HomeErrorCode;
import org.example.gudyeeday.domain.home.repository.WeeklyPhotoRepository;
import org.example.gudyeeday.domain.mission.repository.UserMissionRepository;
import org.example.gudyeeday.domain.user.entity.User;
import org.example.gudyeeday.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HomeServiceTest {

    private static final String EMAIL = "user@test.com";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String IMAGE_URL = "https://bucket.s3.ap-northeast-2.amazonaws.com/weekly-photos/new.jpg";

    private final WeeklyPhotoRepository weeklyPhotoRepository = mock(WeeklyPhotoRepository.class);
    private final UserMissionRepository userMissionRepository = mock(UserMissionRepository.class);
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

    private HomeService homeService(LocalDateTime now) {
        Clock clock = Clock.fixed(now.atZone(KST).toInstant(), KST);
        return new HomeService(weeklyPhotoRepository, userMissionRepository, userRepository, s3Uploader, clock);
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
