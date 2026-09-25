package org.example.gudyeeday.domain.mission.service;

import org.example.gudyeeday.common.exception.CustomException;
import org.example.gudyeeday.domain.mission.dto.request.CustomMissionCreateRequest;
import org.example.gudyeeday.domain.mission.dto.response.BookmarkDetailResponse;
import org.example.gudyeeday.domain.mission.dto.response.BookmarkItemResponse;
import org.example.gudyeeday.domain.mission.dto.response.BookmarkListResponse;
import org.example.gudyeeday.domain.mission.entity.Mission;
import org.example.gudyeeday.domain.mission.entity.MissionBookmark;
import org.example.gudyeeday.domain.mission.entity.UserMission;
import org.example.gudyeeday.domain.mission.enums.BookmarkFilter;
import org.example.gudyeeday.domain.mission.enums.BookmarkMissionStatus;
import org.example.gudyeeday.domain.mission.enums.DayType;
import org.example.gudyeeday.domain.mission.enums.Season;
import org.example.gudyeeday.domain.mission.exception.MissionErrorCode;
import org.example.gudyeeday.domain.mission.repository.MissionBookmarkRepository;
import org.example.gudyeeday.domain.mission.repository.MissionRepository;
import org.example.gudyeeday.domain.mission.repository.UserMissionRepository;
import org.example.gudyeeday.domain.user.entity.User;
import org.example.gudyeeday.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class BookmarkServiceTest {

    private static final String EMAIL = "user@test.com";

    private final MissionBookmarkRepository missionBookmarkRepository = mock(MissionBookmarkRepository.class);
    private final MissionRepository missionRepository = mock(MissionRepository.class);
    private final UserMissionRepository userMissionRepository = mock(UserMissionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final MissionService missionService = mock(MissionService.class);

    // 2026-09-26 01:00 KST
    private final Clock clock = Clock.fixed(ZonedDateTime.of(2026, 9, 26, 1, 0, 0, 0, ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));

    private BookmarkService bookmarkService;
    private User user;

    // 저장만 함(8.29 저장) / 진행중(8.30 저장) / 완료(8.28 저장, 9.02 완료)
    private MissionBookmark saved;
    private MissionBookmark inProgress;
    private MissionBookmark completed;

    @BeforeEach
    void setUp() {
        bookmarkService = new BookmarkService(missionBookmarkRepository, missionRepository, userMissionRepository, userRepository, missionService, clock);

        user = User.createSocialUser(EMAIL, "사용자", "google-1");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        saved = bookmark(100L, mission(1L), LocalDateTime.of(2026, 8, 29, 9, 0));
        inProgress = bookmark(101L, mission(2L), LocalDateTime.of(2026, 8, 30, 9, 0));
        completed = bookmark(102L, mission(3L), LocalDateTime.of(2026, 8, 28, 9, 0));

        UserMission inProgressRecord = UserMission.startMission(user, inProgress.getMission());
        UserMission oldCompletion = UserMission.startMission(user, completed.getMission());
        oldCompletion.complete(LocalDateTime.of(2026, 8, 31, 20, 0));
        UserMission latestCompletion = UserMission.startMission(user, completed.getMission());
        latestCompletion.complete(LocalDateTime.of(2026, 9, 2, 20, 0));

        when(missionBookmarkRepository.findAllWithMissionByUserId(1L)).thenReturn(List.of(inProgress, saved, completed));
        when(userMissionRepository.findByUserIdAndMissionIdIn(1L, List.of(2L, 1L, 3L)))
                .thenReturn(List.of(inProgressRecord, oldCompletion, latestCompletion));
    }

    @Test
    void 전체_탭은_모든_저장_미션과_상태별_날짜를_반환한다() {
        BookmarkListResponse result = bookmarkService.getBookmarks(EMAIL, BookmarkFilter.ALL);

        assertThat(result.totalCount()).isEqualTo(3);
        assertThat(result.bookmarks())
                .extracting(BookmarkItemResponse::bookmarkId, BookmarkItemResponse::status, BookmarkItemResponse::date)
                .containsExactly(
                        tuple(101L, BookmarkMissionStatus.IN_PROGRESS, "8.30"),
                        tuple(100L, BookmarkMissionStatus.SAVED, "8.29"),
                        tuple(102L, BookmarkMissionStatus.COMPLETED, "9.02")  // 가장 최근 완료일
                );
    }

    @Test
    void 남은_미션_탭은_완료하지_않은_미션만_반환하고_전체_개수는_유지한다() {
        BookmarkListResponse result = bookmarkService.getBookmarks(EMAIL, BookmarkFilter.REMAINING);

        assertThat(result.totalCount()).isEqualTo(3);
        assertThat(result.bookmarks()).extracting(BookmarkItemResponse::bookmarkId).containsExactly(101L, 100L);
    }

    @Test
    void 완료한_미션_탭은_완료한_미션만_반환한다() {
        BookmarkListResponse result = bookmarkService.getBookmarks(EMAIL, BookmarkFilter.COMPLETED);

        assertThat(result.bookmarks()).extracting(BookmarkItemResponse::bookmarkId).containsExactly(102L);
    }

    @Test
    void 보관함이_비어있으면_0개를_반환한다() {
        when(missionBookmarkRepository.findAllWithMissionByUserId(1L)).thenReturn(List.of());

        BookmarkListResponse result = bookmarkService.getBookmarks(EMAIL, BookmarkFilter.ALL);

        assertThat(result.totalCount()).isZero();
        assertThat(result.bookmarks()).isEmpty();
        verify(userMissionRepository, never()).findByUserIdAndMissionIdIn(anyLong(), any());
    }

    @Test
    void 상세는_저장날짜_제목_설명을_반환한다() {
        when(missionBookmarkRepository.findWithMissionByIdAndUserId(100L, 1L)).thenReturn(Optional.of(saved));

        BookmarkDetailResponse result = bookmarkService.getBookmark(EMAIL, 100L);

        assertThat(result.savedDate()).isEqualTo("8.29");
        assertThat(result.title()).isEqualTo("미션1");
        assertThat(result.description()).isEqualTo("설명1");
        assertThat(result.custom()).isFalse();
    }

    @Test
    void 본인_보관함에_없는_항목은_BOOKMARK_NOT_FOUND() {
        when(missionBookmarkRepository.findWithMissionByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookmarkService.getBookmark(EMAIL, 999L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(MissionErrorCode.BOOKMARK_NOT_FOUND);
        assertThatThrownBy(() -> bookmarkService.deleteBookmark(EMAIL, 999L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(MissionErrorCode.BOOKMARK_NOT_FOUND);
        assertThatThrownBy(() -> bookmarkService.startBookmarkedMission(EMAIL, 999L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(MissionErrorCode.BOOKMARK_NOT_FOUND);
        verify(missionService, never()).startMission(any(), any());
    }

    @Test
    void 나만의_굳이_미션을_추가하면_미션_생성_후_보관함에_저장된다() {
        when(missionRepository.save(any(Mission.class))).thenAnswer(inv -> {
            Mission m = inv.getArgument(0);
            ReflectionTestUtils.setField(m, "id", 50L);
            return m;
        });
        when(missionBookmarkRepository.save(any(MissionBookmark.class))).thenAnswer(inv -> {
            MissionBookmark b = inv.getArgument(0);
            ReflectionTestUtils.setField(b, "id", 200L);
            return b;
        });

        BookmarkDetailResponse result = bookmarkService.createCustomMission(EMAIL, new CustomMissionCreateRequest("  동네 산책  ", " 굳이 돌아가기 "));

        assertThat(result.bookmarkId()).isEqualTo(200L);
        assertThat(result.missionId()).isEqualTo(50L);
        assertThat(result.title()).isEqualTo("동네 산책");
        assertThat(result.description()).isEqualTo("굳이 돌아가기");
        assertThat(result.savedDate()).isEqualTo("9.26");
        assertThat(result.custom()).isTrue();
        verify(missionRepository).save(argThat(m -> m.getCreator() == user));
    }

    @Test
    void 보관함에서_삭제하면_소프트_삭제된다() {
        when(missionBookmarkRepository.findWithMissionByIdAndUserId(100L, 1L)).thenReturn(Optional.of(saved));

        bookmarkService.deleteBookmark(EMAIL, 100L);

        assertThat(saved.getDeletedAt()).isEqualTo(LocalDateTime.of(2026, 9, 26, 1, 0));
        verify(missionBookmarkRepository, never()).delete(any());
    }

    @Test
    void 이_미션으로_시작하기는_기존_미션_시작_로직을_사용한다() {
        when(missionBookmarkRepository.findWithMissionByIdAndUserId(100L, 1L)).thenReturn(Optional.of(saved));

        bookmarkService.startBookmarkedMission(EMAIL, 100L);

        verify(missionService).startMission(EMAIL, 1L);
    }

    private Mission mission(Long id) {
        Mission m = Mission.createMission("미션" + id, "설명" + id, DayType.ALL, Season.ALL);
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    private MissionBookmark bookmark(Long id, Mission mission, LocalDateTime savedAt) {
        MissionBookmark b = MissionBookmark.createMissionBookmark(user, mission, savedAt);
        ReflectionTestUtils.setField(b, "id", id);
        return b;
    }

    private static org.assertj.core.groups.Tuple tuple(Object... values) {
        return org.assertj.core.groups.Tuple.tuple(values);
    }
}
