package com.example.lms.otp.service;

import com.example.lms.auth.events.UserVerifiedEvent;
import com.example.lms.otp.controllers.VerifyOtpRequest;
import com.example.lms.shared.events.DomainEventPublisher;
import com.example.lms.shared.events.EventNames;
import com.example.lms.users.controllers.UserResponse;
import com.example.lms.users.model.LibraryUser;
import com.example.lms.users.repository.LibraryUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class OtpService {

    private static final String SIGNUP_OTP_KEY_PREFIX = "otp:signup:";

    private final StringRedisTemplate redisTemplate;
    private final OtpHasher otpHasher;
    private final OtpCodeGenerator otpCodeGenerator;
    private final LibraryUserRepository userRepository;
    private final DomainEventPublisher eventPublisher;
    private final Duration otpTtl;

    public OtpService(
            StringRedisTemplate redisTemplate,
            OtpHasher otpHasher,
            OtpCodeGenerator otpCodeGenerator,
            LibraryUserRepository userRepository,
            DomainEventPublisher eventPublisher,
            @Value("${lms.otp.ttl-minutes:10}") long ttlMinutes
    ) {
        this.redisTemplate = redisTemplate;
        this.otpHasher = otpHasher;
        this.otpCodeGenerator = otpCodeGenerator;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.otpTtl = Duration.ofMinutes(ttlMinutes);
    }

    public String createSignupOtp(String email) {
        String normalizedEmail = normalizeEmail(email);
        String otp = otpCodeGenerator.generate();
        redisTemplate.opsForValue().set(
                signupOtpKey(normalizedEmail),
                otpHasher.hash(normalizedEmail, otp),
                otpTtl
        );
        return otp;
    }

    @Transactional
    public UserResponse verifySignupOtp(VerifyOtpRequest request) {
        String email = normalizeEmail(request.email());
        LibraryUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.isEmailVerified()) {
            throw new IllegalStateException("User email is already verified");
        }

        String key = signupOtpKey(email);
        String storedHash = redisTemplate.opsForValue().get(key);
        if (storedHash == null) {
            throw new IllegalArgumentException("OTP is expired or missing");
        }

        String submittedHash = otpHasher.hash(email, request.otp());
        if (!storedHash.equals(submittedHash)) {
            throw new IllegalArgumentException("OTP is invalid");
        }

        user.markEmailVerified();
        redisTemplate.delete(key);

        eventPublisher.publishAfterCommit(
                EventNames.USER_VERIFIED,
                email,
                new UserVerifiedEvent(user.getUserId(), email, Instant.now())
        );

        return UserResponse.from(user);
    }

    private String signupOtpKey(String email) {
        return SIGNUP_OTP_KEY_PREFIX + email;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
