package org.example.gudyeeday.domain.user.dto.response;

public record EmailCodeSendResponse(
        String email,
        int expiresIn
) {
}
