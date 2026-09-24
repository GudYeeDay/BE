package org.example.gudyeeday.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gudyeeday.common.exception.CustomException;
import org.example.gudyeeday.common.response.ErrorCode;
import org.example.gudyeeday.domain.user.dto.request.EmailCodeSendRequest;
import org.example.gudyeeday.domain.user.dto.request.EmailVerificationRequest;
import org.example.gudyeeday.domain.user.dto.response.EmailCodeSendResponse;
import org.example.gudyeeday.domain.user.dto.response.EmailVerificationResponse;
import org.example.gudyeeday.domain.user.entity.EmailVerification;
import org.example.gudyeeday.domain.user.exception.AuthErrorCode;
import org.example.gudyeeday.domain.user.repository.EmailVerificationRepository;
import org.example.gudyeeday.domain.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailVerificationService {

    private static final int CODE_EXPIRE_SECONDS = 300;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender javaMailSender;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Transactional
    public EmailCodeSendResponse sendVerificationCode(EmailCodeSendRequest request) {
        String email = normalizeEmail(request.email());

//        if (userRepository.existsByEmail(email)) {
//            throw new CustomException(AuthErrorCode.DUPLICATE_EMAIL);
//        }

        EmailVerification verification = emailVerificationRepository.findByEmail(email)
                .orElse(null);

        if (verification != null
                && !verification.canResendAfter(RESEND_COOLDOWN_SECONDS)) {
            throw new CustomException(AuthErrorCode.TOO_MANY_VERIFICATION_REQUESTS);
        }

        String code = generateCode();
        String encryptedCode = passwordEncoder.encode(code);
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(CODE_EXPIRE_SECONDS);

        if (verification == null) {
            verification = EmailVerification.createEmailVerification(
                    email,
                    encryptedCode,
                    expiresAt
            );
        } else {
            verification.reissueCode(encryptedCode, expiresAt);
        }

        emailVerificationRepository.save(verification);
        sendMail(email, code);

        return new EmailCodeSendResponse(email, CODE_EXPIRE_SECONDS);
    }

    @Transactional
    public EmailVerificationResponse verifyCode(EmailVerificationRequest request) {
        String email = normalizeEmail(request.email());

        EmailVerification verification = emailVerificationRepository.findByEmail(email)
                .orElseThrow(() ->
                        new CustomException(AuthErrorCode.INVALID_VERIFICATION_CODE)
                );

        if (verification.isExpired()) {
            throw new CustomException(AuthErrorCode.EXPIRED_VERIFICATION_CODE);
        }

        String inputCode = request.code().trim().toUpperCase(Locale.ROOT);

        if (!passwordEncoder.matches(inputCode, verification.getCode())) {
            throw new CustomException(AuthErrorCode.INVALID_VERIFICATION_CODE);
        }

        verification.verify();

        return new EmailVerificationResponse(email, true);
    }

    private void sendMail(String recipientEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(recipientEmail);
            message.setSubject("[GUDYEEDAY] 이메일 인증번호");
            message.setText("""
                    안녕하세요. GUDYEEDAY입니다.

                    이메일 인증번호는 %s 입니다.
                    인증번호는 5분간 유효합니다.
                    """.formatted(code));

            javaMailSender.send(message);
        } catch (MailException e) {
            log.error("인증 메일 발송 실패: {}", recipientEmail, e);
            throw new CustomException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "인증 메일 발송에 실패했습니다."
            );
        }
    }

    private static final String CODE_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder(6);

        for (int i = 0; i < 6; i++) {
            int index = random.nextInt(CODE_CHARACTERS.length());
            code.append(CODE_CHARACTERS.charAt(index));
        }

        return code.toString();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
