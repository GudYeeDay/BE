package org.example.gudyeeday.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

// 애플 로그인 identity token 검증
// - 서명: 애플 공개키(https://appleid.apple.com/auth/keys)로 확인 (키는 캐시하고, 바뀌면 다시 받아옴)
// - iss: https://appleid.apple.com / aud: 우리 앱 ID(apple.oauth.client-ids) / exp: 만료 안 됨
@Component
public class AppleIdTokenVerifier {

    private static final String ISSUER = "https://appleid.apple.com";
    private static final String JWK_SET_URI = "https://appleid.apple.com/auth/keys";

    private final NimbusJwtDecoder jwtDecoder;

    public AppleIdTokenVerifier(@Value("${apple.oauth.client-ids:}") String clientIds) {
        Set<String> audiences = Arrays.stream(clientIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toUnmodifiableSet());

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(ISSUER),
                new JwtClaimValidator<Collection<String>>(
                        JwtClaimNames.AUD,
                        aud -> aud != null && aud.stream().anyMatch(audiences::contains)
                )
        ));
        this.jwtDecoder = decoder;
    }

    // 검증에 실패하면 예외(JwtException)를 던짐
    public AppleIdToken verify(String identityToken) {
        Jwt jwt = jwtDecoder.decode(identityToken);
        return new AppleIdToken(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                isTrue(jwt.getClaim("email_verified"))
        );
    }

    // 애플은 email_verified를 true 또는 "true"로 줌
    private static boolean isTrue(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    public record AppleIdToken(String subject, String email, boolean emailVerified) {
    }
}