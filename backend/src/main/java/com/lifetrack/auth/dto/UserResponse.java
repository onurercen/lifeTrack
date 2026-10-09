package com.lifetrack.auth.dto;

import com.lifetrack.user.entity.User;

/** [emailVerified] false means the client should ask for the code sent by e-mail. */
public record UserResponse(Long id, String name, String email, boolean emailVerified) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.isEmailVerified());
    }
}
