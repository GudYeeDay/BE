package org.example.gudyeeday.domain.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceUpdateNotificationRequest(
        @Schema(description = "알림함에 보일 제목 (비우면 '서비스 업데이트')", example = "굳이가 더 편리해졌어요")
        @Size(max = 100)
        String title,

        @Schema(description = "알림함 상세에 보일 본문 (업데이트 내용)", example = "이제 완료한 굳이를 캘린더에서 한눈에 볼 수 있어요.")
        @NotBlank
        @Size(max = 500)
        String content
) {
}