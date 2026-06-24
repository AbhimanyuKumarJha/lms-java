package com.example.lms.users.controllers;

import com.example.lms.users.model.LibraryUser;

import java.io.Serializable;
import java.time.LocalDateTime;

public record UserResponse(
        Long userId,
        String name,
        String email,
        boolean emailVerified,
        String role,
        LocalDateTime createdAt
) implements Serializable {

    public static UserResponse from(LibraryUser user) {
        return new UserResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
