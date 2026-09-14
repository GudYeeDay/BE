package org.example.gudyeeday.domain.user.dto.response;

import org.example.gudyeeday.domain.user.entity.User;

public record GoogleLoginResponse(
        Long userId,
        String name,
        String accessToken,
        String refreshToken,
        boolean isNewUser
) {
    public static GoogleLoginResponse of(
            User user,
            TokenRefreshResponse tokenResponse,
            boolean isNewUser
    ) {
        return new GoogleLoginResponse(
                user.getId(),
                user.getName(),
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                isNewUser
        );
    }
}