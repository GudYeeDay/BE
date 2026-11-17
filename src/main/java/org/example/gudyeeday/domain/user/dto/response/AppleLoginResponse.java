package org.example.gudyeeday.domain.user.dto.response;

import org.example.gudyeeday.domain.user.entity.User;
import org.example.gudyeeday.domain.user.enums.Provider;

public record AppleLoginResponse(
        Long userId,
        String name,
        String email,
        Provider provider,
        String accessToken,
        String refreshToken,
        boolean isNewUser
) {
    public static AppleLoginResponse of(
            User user,
            TokenRefreshResponse tokenResponse,
            boolean isNewUser
    ) {
        return new AppleLoginResponse(
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