package org.example.gudyeeday.domain.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceTokenRequest(
        @Schema(description = "앱에서 Firebase로 발급받은 FCM 기기 토큰")
        @NotBlank
        @Size(max = 512)
        String token
) {
}