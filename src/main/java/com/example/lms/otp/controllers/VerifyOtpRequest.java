package com.example.lms.otp.controllers;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @Email @NotBlank String email,
        @Pattern(regexp = "\\d{6}") String otp
) {
}
