package org.example.gudyeeday.domain.user.dto.response;

import org.example.gudyeeday.domain.user.entity.User;

public record RoleChangeResponse(
        Long userId,
        String name
) {
    public static RoleChangeResponse of(User user) {
        return new RoleChangeResponse(
                user.getId(),
                user.getName()
        );
    }
}