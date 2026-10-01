package org.example.gudyeeday.domain.mission.service;

import lombok.RequiredArgsConstructor;
import org.example.gudyeeday.common.exception.CustomException;
import org.example.gudyeeday.domain.mission.dto.request.CustomMissionCreateRequest;
import org.example.gudyeeday.domain.mission.dto.response.BookmarkDetailResponse;
import org.example.gudyeeday.domain.mission.dto.response.BookmarkItemResponse;
import org.example.gudyeeday.domain.mission.dto.response.BookmarkListResponse;
import org.example.gudyeeday.domain.mission.dto.response.InProgressMissionResponse;
import org.example.gudyeeday.domain.mission.entity.Mission;
import org.example.gudyeeday.domain.mission.entity.MissionBookmark;
import org.example.gudyeeday.domain.mission.entity.UserMission;
import org.example.gudyeeday.domain.mission.enums.BookmarkFilter;
import org.example.gudyeeday.domain.mission.enums.BookmarkMissionStatus;
import org.example.gudyeeday.domain.mission.enums.UserMissionStatus;
import org.example.gudyeeday.domain.mission.exception.MissionErrorCode;
import org.example.gudyeeday.domain.mission.repository.MissionBookmarkRepository;
import org.example.gudyeeday.domain.mission.repository.MissionRepository;
import org.example.gudyeeday.domain.mission.repository.UserMissionRepository;
import org.example.gudyeeday.domain.user.entity.User;
import org.example.gudyeeday.domain.user.exception.AuthErrorCode;
import org.example.gudyeeday.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final MissionBookmarkRepository missionBookmarkRepository;
    private final MissionRepository missionRepository;
    private final UserMissionRepository userMissionRepository;
    private final UserRepository userRepository;
    private final MissionService missionService;
    private final Clock clock;

    // 보관함 목록 (탭: 전체/남은 미션/완료한 미션)
    public BookmarkListResponse getBookmarks(String email, BookmarkFilter filter) {
        User user = getUser(email);
        List<MissionBookmark> bookmarks = missionBookmarkRepository.findAllWithMissionByUserId(user.getId());

        List<Long> missionIds = bookmarks.stream().map(b -> b.getMission().getId()).toList();
        Map<Long, List<UserMission>> userMissionsByMission = missionIds.isEmpty()
                ? Map.of()
                : userMissionRepository.findByUserIdAndMissionIdIn(user.getId(), missionIds).stream()
                        .collect(Collectors.groupingBy(um -> um.getMission().getId()));

        List<BookmarkItemResponse> items = bookmarks.stream()
                .map(b -> toItem(b, userMissionsByMission.getOrDefault(b.getMission().getId(), List.of())))
                .filter(item -> matches(item, filter))
                .toList();

        return new BookmarkListResponse(bookmarks.size(), items);
    }

    // 저장한 미션 상세
    public BookmarkDetailResponse getBookmark(String email, Long bookmarkId) {
        User user = getUser(email);
        return BookmarkDetailResponse.from(getBookmark(user, bookmarkId));
    }

    // 나만의 굳이 미션 추가 (미션 생성 후 보관함에 저장)
    @Transactional
    public BookmarkDetailResponse createCustomMission(String email, CustomMissionCreateRequest request) {
        User user = getUser(email);

        Mission mission = missionRepository.save(
                Mission.createCustomMission(user, request.title().strip(), request.description().strip())
        );
        MissionBookmark bookmark = missionBookmarkRepository.save(
                MissionBookmark.createMissionBookmark(user, mission, LocalDateTime.now(clock))
        );

        return BookmarkDetailResponse.from(bookmark);
    }

    // 보관함에서 삭제 (소프트 삭제, 모든 탭에서 사라짐)
    @Transactional
    public void deleteBookmark(String email, Long bookmarkId) {
        User user = getUser(email);
        getBookmark(user, bookmarkId).delete(LocalDateTime.now(clock));
    }

    // 저장한 미션으로 시작 (진행중인 미션이 없을 때만 가능)
    @Transactional
    public InProgressMissionResponse startBookmarkedMission(String email, Long bookmarkId) {
        User user = getUser(email);
        Long missionId = getBookmark(user, bookmarkId).getMission().getId();
        return missionService.startMission(email, missionId);
    }

    // 진행중 기록이 있으면 IN_PROGRESS, 완료 기록이 있으면 가장 최근 완료일 기준 COMPLETED, 없으면 SAVED
    private BookmarkItemResponse toItem(MissionBookmark bookmark, List<UserMission> userMissions) {
        boolean inProgress = userMissions.stream()
                .anyMatch(um -> um.getStatus() == UserMissionStatus.IN_PROGRESS);
        if (inProgress) {
            return BookmarkItemResponse.of(bookmark, BookmarkMissionStatus.IN_PROGRESS, bookmark.getSavedAt().toLocalDate());
        }

        return userMissions.stream()
                .filter(um -> um.getStatus() == UserMissionStatus.COMPLETED)
                .map(UserMission::getCompletedAt)
                .max(Comparator.naturalOrder())
                .map(completedAt -> BookmarkItemResponse.of(bookmark, BookmarkMissionStatus.COMPLETED, completedAt.toLocalDate()))
                .orElseGet(() -> BookmarkItemResponse.of(bookmark, BookmarkMissionStatus.SAVED, bookmark.getSavedAt().toLocalDate()));
    }

    private boolean matches(BookmarkItemResponse item, BookmarkFilter filter) {
        return switch (filter) {
            case ALL -> true;
            case REMAINING -> item.status() != BookmarkMissionStatus.COMPLETED;
            case COMPLETED -> item.status() == BookmarkMissionStatus.COMPLETED;
        };
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
    }

    private MissionBookmark getBookmark(User user, Long bookmarkId) {
        return missionBookmarkRepository.findWithMissionByIdAndUserId(bookmarkId, user.getId())
                .orElseThrow(() -> new CustomException(MissionErrorCode.BOOKMARK_NOT_FOUND));
    }
}
