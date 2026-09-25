package org.example.gudyeeday.domain.user.dto.response;

import org.example.gudyeeday.domain.user.entity.User;
import org.example.gudyeeday.domain.user.enums.Provider;

public record GoogleLoginResponse(
        Long userId,
        String name,
        String email,
        Provider provider,
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
                user.getEmail(),
                user.getProvider(),
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                isNewUser
        );
    }
}