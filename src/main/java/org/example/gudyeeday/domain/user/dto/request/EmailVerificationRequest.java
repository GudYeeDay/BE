package org.example.gudyeeday.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailVerificationRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "인증번호는 필수입니다.")
        @Pattern(
                regexp = "^[A-HJ-NP-Z2-9]{6}$",
                message = "인증번호는 영문 대문자와 숫자로 구성된 6자리여야 합니다."
        )
        String code
) {
}
