package com.example.lms.borrowing.controllers;

import jakarta.validation.constraints.NotNull;

public record CreateIssueRequest(
        @NotNull Long transactionId,
        @NotNull Long userId,
        @NotNull Long bookCopyId
) {
}
