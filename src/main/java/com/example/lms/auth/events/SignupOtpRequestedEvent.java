package com.example.lms.auth.events;

import java.time.Instant;

public record SignupOtpRequestedEvent(
        Long userId,
        String email,
        String otp,
        Instant occurredAt
) {
}
