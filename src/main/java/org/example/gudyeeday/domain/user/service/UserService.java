package org.example.gudyeeday.domain.user.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import lombok.RequiredArgsConstructor;
import org.example.gudyeeday.common.exception.CustomException;
import org.example.gudyeeday.config.jwt.JwtTokenProvider;
import org.example.gudyeeday.domain.user.dto.request.*;
import org.example.gudyeeday.domain.user.dto.response.EmailVerificationResponse;
import org.example.gudyeeday.domain.user.dto.response.GoogleLoginResponse;
import org.example.gudyeeday.domain.user.dto.response.TokenRefreshResponse;
import org.example.gudyeeday.domain.user.dto.response.UserResponse;
import org.example.gudyeeday.domain.user.entity.EmailVerification;
import org.example.gudyeeday.domain.user.entity.RefreshToken;
import org.example.gudyeeday.domain.user.entity.User;
import org.example.gudyeeday.domain.user.enums.Provider;
import org.example.gudyeeday.domain.user.exception.AuthErrorCode;
import org.example.gudyeeday.domain.user.repository.EmailVerificationRepository;
import org.example.gudyeeday.domain.user.repository.RefreshTokenRepository;
import org.example.gudyeeday.domain.user.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailVerificationRepository emailVerificationRepository;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final EmailVerificationService emailVerificationService;

    @Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private UserService self;

    @Transactional
    public UserResponse signup(SignupRequest request) {
        String email = request.email().trim().toLowerCase();

        if (!request.password().equals(request.passwordCheck())) {
            throw new CustomException(AuthErrorCode.PASSWORD_MISMATCH);
        }

        if (userRepository.existsByEmail(email)) {
            throw new CustomException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        validateEmailVerified(email);

        User user = userRepository.save(
                User.createLocalUser(
                        email,
                        passwordEncoder.encode(request.password()),
                        request.name().trim()
                )
        );

        TokenRefreshResponse tokens = issueTokens(user);

        return UserResponse.of(
                user,
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.accessTokenExpiresIn()
        );
    }

    @Transactional
    public UserResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_CREDENTIALS));

        if (!user.hasPassword()) {
            throw new CustomException(AuthErrorCode.SOCIAL_LOGIN_REQUIRED);
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        TokenRefreshResponse tokens = issueTokens(user);

        return UserResponse.of(
                user,
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.accessTokenExpiresIn()
        );
    }

    @Transactional
    public TokenRefreshResponse reissue(TokenRefreshRequest request) {
        String refreshTokenValue = request.refreshToken();

        if (!jwtTokenProvider.validateToken(refreshTokenValue)
                || !jwtTokenProvider.isRefreshToken(refreshTokenValue)) {
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findByEmail(jwtTokenProvider.getEmail(refreshTokenValue))
                .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        RefreshToken savedRefreshToken = refreshTokenRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        if (savedRefreshToken.isExpired()
                || !savedRefreshToken.getToken().equals(hashToken(refreshTokenValue))) {
            refreshTokenRepository.delete(savedRefreshToken);
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        return issueTokens(user);
    }

    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(AuthErrorCode.UNAUTHORIZED));

        refreshTokenRepository.deleteByUser(user);
    }

    private void validateEmailVerified(String email) {
        EmailVerification verification = emailVerificationRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(AuthErrorCode.EMAIL_NOT_VERIFIED));

        if (!verification.isVerified()) {
            throw new CustomException(AuthErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    private TokenRefreshResponse issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getEmail()
        );

        String refreshTokenValue = jwtTokenProvider.createRefreshToken(user.getEmail());

        /*
         * 한 사용자당 하나의 로그인 세션만 유지한다.
         * 새 로그인 또는 재발급 시 기존 Refresh Token은 폐기된다.
         */
        refreshTokenRepository.deleteByUser(user);

        refreshTokenRepository.save(
                RefreshToken.createRefreshToken(
                        hashToken(refreshTokenValue),
                        user,
                        LocalDateTime.now()
                                .plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000)
                )
        );

        return TokenRefreshResponse.of(
                accessToken,
                refreshTokenValue,
                jwtTokenProvider.getAccessTokenExpiration()
        );
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public GoogleLoginResponse googleLogin(GoogleLoginRequest request) {
        GoogleIdToken googleIdToken;

        try {
            googleIdToken = googleIdTokenVerifier.verify(request.idToken());
        } catch (Exception e) {
            throw new CustomException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }

        if (googleIdToken == null) {
            throw new CustomException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }

        GoogleIdToken.Payload payload = googleIdToken.getPayload();

        String providerId = payload.getSubject(); // Google sub: 고유 식별자
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String googleImageUrl = (String) payload.get("picture");

        /*
         * emailVerified 검증은 이메일 기반 자동 연동의 전제 조건이다.
         * 구글이 소유권을 확인해준 이메일이어야만 기존 계정에 연동할 수 있다.
         */
        if (providerId == null
                || email == null
                || !Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new CustomException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }

        // signup/login과 동일한 규칙으로 정규화. 대소문자/공백 차이로 인한
        // 중복 계정 생성 및 JWT subject 불일치를 막기 위함.
        email = email.trim().toLowerCase();

        /*
         * 구글 식별자로도, 이메일로도 매칭되는 계정이 없을 때만 진짜 신규 가입이다.
         * 기존 계정 연동인 경우에는 프로필 이미지를 덮어쓰지 않는다.
         */
        boolean needsNewAccount =
                userRepository.findByProviderId(providerId).isEmpty()
                        && !userRepository.existsByEmail(email);

        return self.processGoogleLogin(providerId, email, name);
    }

    @Transactional
    protected GoogleLoginResponse processGoogleLogin(
            String providerId,
            String email,
            String name
    ) {

        // 1) 이미 구글과 연동된 계정 -> 그대로 로그인
        User user = userRepository.findByProviderId(providerId).orElse(null);

        boolean isNewUser = false;

        if (user == null) {
            User existing = userRepository.findByEmail(email).orElse(null);

            if (existing != null) {
                /*
                 * 2) 동일 이메일의 기존 계정이 있으면 구글 계정을 연동하고
                 *    기존 계정으로 로그인시킨다. user_id가 유지되므로
                 *    다른 도메인이 참조하는 FK도 그대로 살아있다.
                 */
                if (existing.hasGoogleLinked()) {
                    // 이메일은 같은데 구글 sub이 다른 비정상 케이스
                    throw new CustomException(AuthErrorCode.ALREADY_LINKED_GOOGLE_ACCOUNT);
                }

                existing.linkGoogleAccount(providerId);
                user = existing;

            } else {
                // 3) 완전 신규 -> 소셜 계정 생성
                try {
                    user = userRepository.save(
                            User.createSocialUser(
                                    email,
                                    name == null || name.isBlank() ? "사용자" : name,
                                    providerId
                            )
                    );
                } catch (DataIntegrityViolationException e) {
                    // 동시 요청으로 그 사이에 동일 이메일 계정이 생성된 경우
                    throw new CustomException(AuthErrorCode.DUPLICATE_EMAIL);
                }

                isNewUser = true;
            }
        }

        TokenRefreshResponse tokenResponse = issueTokens(user);

        return GoogleLoginResponse.of(user,tokenResponse, isNewUser);
    }

    @Transactional
    public void passwordReset(PasswordResetRequest request) {
        if (!request.password().equals(request.passwordCheck())) {
            throw new CustomException(AuthErrorCode.PASSWORD_MISMATCH);
        }

        String email = request.emailVerificationRequest().email();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        if(user.getProvider()== Provider.GOOGLE){
            throw new CustomException(AuthErrorCode.SOCIAL_LOGIN_REQUIRED);
        }

        EmailVerificationResponse response = emailVerificationService.verifyCode(request.emailVerificationRequest());
        if (!response.verified()) {
            throw new CustomException(AuthErrorCode.INVALID_VERIFICATION_CODE);
        }

        user.updatePassword(passwordEncoder.encode(request.password()));

        user.getRefreshTokens().clear();

        emailVerificationRepository.deleteByEmail(email);
    }

    private String hashToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }

}

