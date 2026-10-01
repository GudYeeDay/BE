package org.example.gudyeeday.domain.mission.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomMissionCreateRequest(

        @NotBlank(message = "미션 이름은 필수입니다.")
        @Size(max = 100, message = "미션 이름은 100자 이하여야 합니다.")
        String title,

        @NotBlank(message = "간단한 설명은 필수입니다.")
        @Size(max = 500, message = "간단한 설명은 500자 이하여야 합니다.")
        String description
) {
}
