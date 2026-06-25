package com.example.lms.borrowing.events;

import java.time.Instant;
import java.time.LocalDate;

public record BookReturnedEvent(
        Long transactionId,
        Long userId,
        Long bookCopyId,
        String title,
        LocalDate returnedAt,
        Instant occurredAt
) {
}
