package org.example.gudyeeday.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gudyeeday.common.exception.CustomException;
import org.example.gudyeeday.config.AppleAuthClient;
import org.example.gudyeeday.domain.user.dto.request.WithdrawRequest;
import org.example.gudyeeday.domain.user.entity.User;
import org.example.gudyeeday.domain.user.exception.AuthErrorCode;
import org.example.gudyeeday.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawService {

    private final UserRepository userRepository;
    private final AppleAuthClient appleAuthClient;
    private final UserDataDeleter userDataDeleter;

    public void withdraw(String email, WithdrawRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        // 구글은 서버가 가진 구글 토큰이 없어서 해제 요청 없이 계정과 함께 연동 정보만 삭제
        if (user.hasAppleLinked()) {
            String code = request == null ? null : request.appleAuthorizationCode();
            if (!StringUtils.hasText(code)) {
                throw new CustomException(AuthErrorCode.APPLE_AUTH_CODE_REQUIRED);
            }
            appleAuthClient.revoke(code, user.getAppleId());
        }

        userDataDeleter.deleteAll(user.getId(), user.getEmail());
        log.info("회원 탈퇴 완료: userId={}", user.getId());
    }
}
