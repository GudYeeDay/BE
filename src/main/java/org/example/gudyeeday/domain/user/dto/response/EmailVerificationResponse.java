package org.example.gudyeeday.domain.user.dto.response;

public record EmailVerificationResponse(
        String email,
        boolean verified
) {
}
