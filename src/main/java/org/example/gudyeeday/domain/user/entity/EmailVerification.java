package org.example.gudyeeday.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "email_verification_id")
    private Long id;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Builder.Default
    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_sent_at", nullable = false)
    private LocalDateTime lastSentAt;

    // 생성 메서드
    public static EmailVerification createEmailVerification(
            String email,
            String encryptedCode,
            LocalDateTime expiresAt
    ) {
        LocalDateTime now = LocalDateTime.now();

        return EmailVerification.builder()
                .email(email)
                .code(encryptedCode)
                .expiresAt(expiresAt)
                .createdAt(now)
                .lastSentAt(now)
                .build();
    }

    // 인증 완료 처리
    public void verify() {
        this.verified = true;
    }

    // 만료 여부 확인
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    // 코드 재발급
    public void reissueCode(String encryptedCode, LocalDateTime newExpiresAt) {
        this.code = encryptedCode;
        this.expiresAt = newExpiresAt;
        this.lastSentAt = LocalDateTime.now();
        this.verified = false;
    }

    public boolean canResendAfter(long cooldownSeconds) {
        return !this.lastSentAt
                .plusSeconds(cooldownSeconds)
                .isAfter(LocalDateTime.now());
    }
}
