package com.example.lms.users.controllers;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotNull Long userId,
        @NotBlank String name,
        @Email @NotBlank String email,
        @Size(min = 8) String password
) {
}
