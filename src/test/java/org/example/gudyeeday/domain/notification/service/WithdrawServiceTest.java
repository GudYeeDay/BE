package org.example.gudyeeday.domain.notification.service;

import org.example.gudyeeday.common.exception.CustomException;
import org.example.gudyeeday.config.AppleAuthClient;
import org.example.gudyeeday.domain.user.dto.request.WithdrawRequest;
import org.example.gudyeeday.domain.user.entity.User;
import org.example.gudyeeday.domain.user.exception.AuthErrorCode;
import org.example.gudyeeday.domain.user.repository.UserRepository;
import org.example.gudyeeday.domain.user.service.UserDataDeleter;
import org.example.gudyeeday.domain.user.service.WithdrawService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

class WithdrawServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AppleAuthClient appleAuthClient = mock(AppleAuthClient.class);
    private final UserDataDeleter userDataDeleter = mock(UserDataDeleter.class);

    private final WithdrawService withdrawService = new WithdrawService(userRepository, appleAuthClient, userDataDeleter);

    @Test
    void 일반_계정은_애플_해제_없이_데이터를_삭제한다() {
        User user = withId(User.createLocalUser("local@test.com", "encoded", "사용자"), 1L);
        when(userRepository.findByEmail("local@test.com")).thenReturn(Optional.of(user));

        withdrawService.withdraw("local@test.com", null);

        verifyNoInteractions(appleAuthClient);
        verify(userDataDeleter).deleteAll(1L, "local@test.com");
    }

    @Test
    void 구글_계정도_애플_해제_없이_데이터를_삭제한다() {
        User user = withId(User.createSocialUser("google@test.com", "사용자", "google-sub"), 2L);
        when(userRepository.findByEmail("google@test.com")).thenReturn(Optional.of(user));

        withdrawService.withdraw("google@test.com", new WithdrawRequest(null));

        verifyNoInteractions(appleAuthClient);
        verify(userDataDeleter).deleteAll(2L, "google@test.com");
    }

    @Test
    void 애플_계정은_애플_해제_후_데이터를_삭제한다() {
        User user = withId(User.createAppleUser("apple@test.com", "사용자", "apple-sub"), 3L);
        when(userRepository.findByEmail("apple@test.com")).thenReturn(Optional.of(user));

        withdrawService.withdraw("apple@test.com", new WithdrawRequest("auth-code"));

        var inOrder = inOrder(appleAuthClient, userDataDeleter);
        inOrder.verify(appleAuthClient).revoke("auth-code", "apple-sub");
        inOrder.verify(userDataDeleter).deleteAll(3L, "apple@test.com");
    }

    @Test
    void 애플_계정인데_코드가_없으면_아무것도_지우지_않는다() {
        User user = withId(User.createAppleUser("apple@test.com", "사용자", "apple-sub"), 3L);
        when(userRepository.findByEmail("apple@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> withdrawService.withdraw("apple@test.com", null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(AuthErrorCode.APPLE_AUTH_CODE_REQUIRED);

        verifyNoInteractions(appleAuthClient, userDataDeleter);
    }

    @Test
    void 애플_해제가_실패하면_데이터를_지우지_않는다() {
        User user = withId(User.createAppleUser("apple@test.com", "사용자", "apple-sub"), 3L);
        when(userRepository.findByEmail("apple@test.com")).thenReturn(Optional.of(user));
        doThrow(new CustomException(AuthErrorCode.INVALID_APPLE_AUTH_CODE))
                .when(appleAuthClient).revoke(anyString(), anyString());

        assertThatThrownBy(() -> withdrawService.withdraw("apple@test.com", new WithdrawRequest("expired")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(AuthErrorCode.INVALID_APPLE_AUTH_CODE);

        verify(userDataDeleter, never()).deleteAll(anyLong(), any());
    }

    private User withId(User user, Long id) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}