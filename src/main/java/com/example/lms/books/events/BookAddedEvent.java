package com.example.lms.books.events;

import java.time.Instant;

public record BookAddedEvent(
        Long bookId,
        String title,
        String author,
        Instant occurredAt
) {
}
