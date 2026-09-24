package org.example.gudyeeday.domain.user.dto.response;


import org.example.gudyeeday.domain.user.entity.User;

public record SignupResponse(
        Long userId,
        String name,
        String email
) {
    public static SignupResponse from(User user) {
        return new SignupResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}