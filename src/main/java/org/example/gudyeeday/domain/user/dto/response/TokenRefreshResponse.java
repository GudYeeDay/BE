package org.example.gudyeeday.domain.user.dto.response;

public record TokenRefreshResponse (
    String accessToken,
    String refreshToken,
    long accessTokenExpiresIn
){
    public static TokenRefreshResponse of(
            String accessToken,
            String refreshToken,
            long accessTokenExpiresIn
    ) {
        return new TokenRefreshResponse(
                accessToken,
                refreshToken,
                accessTokenExpiresIn
        );
    }
}

