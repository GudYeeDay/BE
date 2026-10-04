package org.example.gudyeeday.domain.complete.dto.request;

import jakarta.validation.constraints.Size;

public record CompleteMissionRequest(
        @Size(max = 200, message = "위치는 200자 이하여야 합니다.")
        String location,

        @Size(max = 500, message = "내용은 500자 이하여야 합니다.")
        String content
) {
}
