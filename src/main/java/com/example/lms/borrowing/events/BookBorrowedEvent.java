package com.example.lms.borrowing.events;

import java.time.Instant;
import java.time.LocalDate;

public record BookBorrowedEvent(
        Long transactionId,
        Long userId,
        Long bookCopyId,
        String title,
        LocalDate issuedAt,
        LocalDate dueDate,
        Instant occurredAt
) {
}
