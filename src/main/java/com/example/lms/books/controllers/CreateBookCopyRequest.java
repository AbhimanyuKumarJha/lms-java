package com.example.lms.books.controllers;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBookCopyRequest(
        @NotNull Long bookCopyId,
        @NotNull Long bookId,
        @NotBlank String copyCode
) {
}
