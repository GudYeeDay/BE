package org.example.gudyeeday.domain.mypage.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PasswordChangeRequest(
        @NotBlank(message = "현재비밀번호는 필수입니다.")
        String currentPassword,

        @NotBlank(message = "새비밀번호는 필수입니다.")
        String newPassword,

        @NotBlank(message = "새비밀번호확인은 필수입니다.")
        String newPasswordCheck
) {
}
