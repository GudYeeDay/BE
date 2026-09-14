package org.example.gudyeeday.domain.user.dto.response;


import org.example.gudyeeday.domain.user.entity.User;

public record UserListResponse (
    Long userId,
    String name,
    String email
) {
    public static UserListResponse from(User user) {
        return new UserListResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}
