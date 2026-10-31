package org.example.gudyeeday.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

// fcm.enabled=true 일 때만 Firebase를 초기화 (로컬/테스트에서는 키 없이 실행 가능)
@Configuration
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "true")
public class FirebaseConfig {

    @Value("${fcm.credentials-path:}")
    private String credentialsPath;

    @Value("${fcm.credentials-base64:}")
    private String credentialsBase64;

    @Bean
    public FirebaseMessaging firebaseMessaging() throws IOException {
        return FirebaseMessaging.getInstance(firebaseApp());
    }

    private FirebaseApp firebaseApp() throws IOException {
        // 재시작(devtools 등)으로 이미 초기화돼 있으면 그대로 사용
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try (InputStream credentials = openCredentials()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentials))
                    .build();
            return FirebaseApp.initializeApp(options);
        }
    }

    private InputStream openCredentials() throws IOException {
        if (StringUtils.hasText(credentialsBase64)) {
            // 줄바꿈이 섞여 있어도 디코딩되도록 MIME 디코더 사용
            return new ByteArrayInputStream(Base64.getMimeDecoder().decode(credentialsBase64.trim()));
        }
        if (StringUtils.hasText(credentialsPath)) {
            return Files.newInputStream(Path.of(credentialsPath.trim()));
        }
        throw new IllegalStateException("fcm.enabled=true 이면 FCM_CREDENTIALS_PATH 또는 FCM_CREDENTIALS_BASE64 를 설정해야 합니다.");
    }
}