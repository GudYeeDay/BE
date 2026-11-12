package org.example.gudyeeday.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.gudyeeday.common.exception.CustomException;
import org.example.gudyeeday.domain.notification.entity.DeviceToken;
import org.example.gudyeeday.domain.notification.repository.DeviceTokenRepository;
import org.example.gudyeeday.domain.user.entity.User;
import org.example.gudyeeday.domain.user.exception.AuthErrorCode;
import org.example.gudyeeday.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    // 기기 토큰 등록 (이미 있는 토큰이면 지금 로그인한 사용자로 옮김)
    @Transactional
    public void registerToken(String email, String token) {
        User user = getUser(email);
        String trimmed = token.trim();

        deviceTokenRepository.findByToken(trimmed).ifPresentOrElse(
                deviceToken -> deviceToken.changeOwner(user),
                () -> deviceTokenRepository.save(DeviceToken.create(user, trimmed))
        );
    }

    // 기기 토큰 해제 (로그아웃/알림 끄기). 없는 토큰이어도 성공 처리
    @Transactional
    public void deleteToken(String email, String token) {
        User user = getUser(email);
        deviceTokenRepository.deleteByUserIdAndToken(user.getId(), token.trim());
    }

    // 만료된 토큰 삭제
    // 알림 저장 트랜잭션이 커밋된 뒤 호출되므로 반드시 새 트랜잭션에서 실행
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteTokens(Collection<String> tokens) {
        deviceTokenRepository.deleteByTokenIn(tokens);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
    }
}