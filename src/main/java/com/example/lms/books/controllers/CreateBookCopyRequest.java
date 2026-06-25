package com.example.lms.books.controllers;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBookCopyRequest(
        @NotNull Long bookCopyId,
        @NotBlank String title,
        @NotBlank String author,
        String description,
        @NotBlank String copyCode
) {
}
