package com.example.lms.auth.controllers;

import com.example.lms.users.model.LibraryUser;

import java.time.LocalDateTime;

public record SignupResponse(
        Long userId,
        String name,
        String email,
        boolean emailVerified,
        LocalDateTime createdAt
) {

    public static SignupResponse from(LibraryUser user) {
        return new SignupResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }
}
