package com.example.lms.auth.events;

import java.time.Instant;

public record UserVerifiedEvent(
        Long userId,
        String email,
        Instant occurredAt
) {
}
