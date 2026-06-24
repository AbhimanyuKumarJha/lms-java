package com.example.lms.borrowing.events;

import java.time.Instant;
import java.time.LocalDate;

public record BookBorrowedEvent(
        Long transactionId,
        Long userId,
        Long bookCopyId,
        Long bookId,
        LocalDate issuedAt,
        LocalDate dueDate,
        Instant occurredAt
) {
}
