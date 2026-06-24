package com.example.lms.books.controllers;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBookRequest(
        @NotNull Long bookId,
        @NotBlank String title,
        @NotBlank String author,
        String description
) {
}
