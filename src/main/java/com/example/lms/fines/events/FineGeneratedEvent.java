package com.example.lms.fines.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record FineGeneratedEvent(
        Long transactionId,
        Long userId,
        BigDecimal amount,
        LocalDate dueDate,
        LocalDate returnedAt,
        Instant occurredAt
) {
}
