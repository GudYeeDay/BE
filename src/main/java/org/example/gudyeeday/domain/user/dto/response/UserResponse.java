package org.example.gudyeeday.domain.user.dto.response;

import org.example.gudyeeday.domain.user.entity.User;

public record UserResponse(
        Long userId,
        String name,
        String email,
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn
) {
    public static UserResponse of(
            User user,
            String accessToken,
            String refreshToken,
            long accessTokenExpiresIn
    ) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                accessToken,
                refreshToken,
                accessTokenExpiresIn
        );
    }
}
