package org.example.gudyeeday.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(
        EmailVerificationRequest emailVerificationRequest,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String passwordCheck
) {
}
