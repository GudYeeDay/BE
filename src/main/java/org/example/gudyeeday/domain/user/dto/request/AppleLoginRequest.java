package org.example.gudyeeday.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AppleLoginRequest(
        @Schema(description = "앱에서 애플 로그인 후 받은 identityToken (JWT)")
        @NotBlank(message = "Apple identity token은 필수입니다.")
        String identityToken,

        @Schema(description = "애플이 최초 로그인 때만 주는 사용자 이름 (없으면 생략)", example = "홍길동")
        String name
) {
}