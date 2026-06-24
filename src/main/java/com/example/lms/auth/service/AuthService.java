package com.example.lms.auth.service;

import com.example.lms.auth.controllers.SignupRequest;
import com.example.lms.auth.controllers.SignupResponse;
import com.example.lms.auth.events.SignupOtpRequestedEvent;
import com.example.lms.otp.service.OtpService;
import com.example.lms.shared.events.DomainEventPublisher;
import com.example.lms.shared.events.EventNames;
import com.example.lms.users.model.LibraryUser;
import com.example.lms.users.repository.LibraryUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    private static final long MIN_USER_ID = 1_000_000_000L;
    private static final long MAX_USER_ID = 9_999_999_999L;

    private final LibraryUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final DomainEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            LibraryUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            OtpService otpService,
            DomainEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        LibraryUser user = userRepository.save(new LibraryUser(
                nextUserId(),
                request.name(),
                email,
                passwordEncoder.encode(request.password())
        ));

        String otp = otpService.createSignupOtp(email);
        eventPublisher.publishAfterCommit(
                EventNames.SIGNUP_OTP_REQUESTED,
                email,
                new SignupOtpRequestedEvent(user.getUserId(), email, otp, Instant.now())
        );

        return SignupResponse.from(user);
    }

    private Long nextUserId() {
        for (int attempts = 0; attempts < 20; attempts++) {
            long candidate = secureRandom.nextLong(MIN_USER_ID, MAX_USER_ID + 1);
            if (!userRepository.existsById(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate unique user id");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
