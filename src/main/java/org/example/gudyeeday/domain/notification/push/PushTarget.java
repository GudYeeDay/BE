package org.example.gudyeeday.domain.notification.push;

// 푸시 받을 사용자와 기기 토큰
public record PushTarget(Long userId, String token) {
}