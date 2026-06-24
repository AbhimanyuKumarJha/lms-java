package com.example.lms.users.events;

import java.time.Instant;

public record UserCreatedEvent(
        Long userId,
        String email,
        String name,
        Instant occurredAt
) {
}
