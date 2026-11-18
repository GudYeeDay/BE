package org.example.gudyeeday.config;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.example.gudyeeday.common.exception.CustomException;
import org.example.gudyeeday.common.response.ErrorCode;
import org.example.gudyeeday.domain.user.exception.AuthErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

// 애플 서버 API 호출 (회원 탈퇴 시 연동 해제)
// 1) 앱이 보낸 authorizationCode를 애플 토큰(refresh_token)으로 교환
// 2) 그 토큰으로 애플에 연동 해제(revoke) 요청
// 애플에 보내는 client_secret은 .p8 비밀키로 서명한 JWT
@Slf4j
@Component
public class AppleAuthClient {

    private static final String APPLE_URL = "https://appleid.apple.com";
    private static final String TOKEN_URL = APPLE_URL + "/auth/token";
    private static final String REVOKE_URL = APPLE_URL + "/auth/revoke";

    private final RestClient restClient = RestClient.create();
    private final AppleIdTokenVerifier appleIdTokenVerifier;

    // 앱에서 받은 코드라서 client_id는 iOS 번들 ID (APPLE_CLIENT_IDS의 첫 번째 값)
    private final String clientId;
    private final String teamId;
    private final String keyId;
    private final String privateKeyBase64;
    // 웹(Services ID)에서 받은 코드를 교환할 때만 필요. 애플 로그인 때 쓴 Return URL과 똑같아야 함 (앱 코드면 비워둠)
    private final String redirectUri;

    public AppleAuthClient(
            AppleIdTokenVerifier appleIdTokenVerifier,
            @Value("${apple.oauth.client-ids:}") String clientIds,
            @Value("${apple.oauth.team-id:}") String teamId,
            @Value("${apple.oauth.key-id:}") String keyId,
            @Value("${apple.oauth.private-key-base64:}") String privateKeyBase64,
            @Value("${apple.oauth.redirect-uri:}") String redirectUri
    ) {
        this.appleIdTokenVerifier = appleIdTokenVerifier;
        this.clientId = Arrays.stream(clientIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .findFirst()
                .orElse("");
        this.teamId = teamId.trim();
        this.keyId = keyId.trim();
        this.privateKeyBase64 = privateKeyBase64.trim();
        this.redirectUri = redirectUri.trim();
    }

    // authorizationCode의 주인이 expectedAppleId인지 확인한 뒤 애플 연동을 해제
    public void revoke(String authorizationCode, String expectedAppleId) {
        String clientSecret = createClientSecret();

        Map<String, Object> tokens = exchangeCode(authorizationCode, clientSecret);

        // 다른 애플 계정으로 받은 코드면 거절
        String appleId;
        try {
            appleId = appleIdTokenVerifier.verify((String) tokens.get("id_token")).subject();
        } catch (Exception e) {
            // 교환은 성공했지만 id_token의 aud가 APPLE_CLIENT_IDS에 없거나 서명 확인 실패
            log.warn("애플 id_token 검증 실패: client_id={}, 이유={}", clientId, e.getMessage());
            throw new CustomException(AuthErrorCode.INVALID_APPLE_AUTH_CODE);
        }
        if (!expectedAppleId.equals(appleId)) {
            throw new CustomException(AuthErrorCode.APPLE_ACCOUNT_MISMATCH);
        }

        String refreshToken = (String) tokens.get("refresh_token");
        if (StringUtils.hasText(refreshToken)) {
            revokeToken(refreshToken, "refresh_token", clientSecret);
        } else {
            revokeToken((String) tokens.get("access_token"), "access_token", clientSecret);
        }
        log.info("애플 연동 해제 완료: appleId={}", appleId);
    }

    private Map<String, Object> exchangeCode(String authorizationCode, String clientSecret) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", authorizationCode.trim());
        form.add("grant_type", "authorization_code");
        if (StringUtils.hasText(redirectUri)) {
            form.add("redirect_uri", redirectUri);
        }

        try {
            Map<String, Object> tokens = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            if (tokens == null || tokens.get("id_token") == null) {
                throw new CustomException(AuthErrorCode.APPLE_REVOKE_FAILED);
            }
            return tokens;
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            log.warn("애플 코드 교환 실패: status={}, body={}, client_id={}, redirect_uri={}",
                    e.getStatusCode(), body, clientId, redirectUri);
            // invalid_client: 코드 문제가 아니라 서버 설정(client_id, Team ID, Key ID, .p8 키) 문제
            if (body != null && body.contains("invalid_client")) {
                throw new CustomException(AuthErrorCode.APPLE_REVOKE_FAILED,
                        "애플이 서버 설정을 거절했습니다(invalid_client). APPLE_CLIENT_IDS 첫 값, APPLE_TEAM_ID, APPLE_KEY_ID, .p8 키를 확인해주세요.");
            }
            // invalid_grant: 만료(5분)됐거나 이미 쓴 코드, 또는 웹 코드인데 redirect_uri가 다름
            if (e.getStatusCode().is4xxClientError()) {
                throw new CustomException(AuthErrorCode.INVALID_APPLE_AUTH_CODE);
            }
            throw new CustomException(AuthErrorCode.APPLE_REVOKE_FAILED);
        } catch (RestClientException e) {
            log.error("애플 코드 교환 요청 실패", e);
            throw new CustomException(AuthErrorCode.APPLE_REVOKE_FAILED);
        }
    }

    private void revokeToken(String token, String tokenTypeHint, String clientSecret) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("token", token);
        form.add("token_type_hint", tokenTypeHint);

        try {
            restClient.post()
                    .uri(REVOKE_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("애플 연동 해제 요청 실패", e);
            throw new CustomException(AuthErrorCode.APPLE_REVOKE_FAILED);
        }
    }

    // 애플 API용 client_secret (ES256 JWT, 5분 유효)
    private String createClientSecret() {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(teamId)
                || !StringUtils.hasText(keyId) || !StringUtils.hasText(privateKeyBase64)) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "애플 연동 해제 설정이 없습니다. (APPLE_CLIENT_IDS, APPLE_TEAM_ID, APPLE_KEY_ID, APPLE_PRIVATE_KEY_BASE64)");
        }

        Instant now = Instant.now();
        return Jwts.builder()
                .header().keyId(keyId).and()
                .issuer(teamId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .audience().single(APPLE_URL)
                .subject(clientId)
                .signWith(loadPrivateKey(), Jwts.SIG.ES256)
                .compact();
    }

    // AuthKey_XXXX.p8 (PEM) 파일을 base64로 넣은 값을 비밀키로 변환
    private PrivateKey loadPrivateKey() {
        try {
            String pem = new String(Base64.getMimeDecoder().decode(privateKeyBase64), StandardCharsets.UTF_8);
            String body = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(body);
            return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "APPLE_PRIVATE_KEY_BASE64 값을 읽을 수 없습니다. AuthKey .p8 파일 전체를 base64로 넣었는지 확인해주세요.");
        }
    }
}
