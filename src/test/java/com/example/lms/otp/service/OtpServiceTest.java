package com.example.lms.otp.service;

import com.example.lms.auth.events.UserVerifiedEvent;
import com.example.lms.otp.controllers.VerifyOtpRequest;
import com.example.lms.shared.events.DomainEventPublisher;
import com.example.lms.shared.events.EventNames;
import com.example.lms.users.model.LibraryUser;
import com.example.lms.users.repository.LibraryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OtpServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private OtpHasher otpHasher;
    private OtpCodeGenerator otpCodeGenerator;
    private LibraryUserRepository userRepository;
    private DomainEventPublisher eventPublisher;
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        otpHasher = mock(OtpHasher.class);
        otpCodeGenerator = mock(OtpCodeGenerator.class);
        userRepository = mock(LibraryUserRepository.class);
        eventPublisher = mock(DomainEventPublisher.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        otpService = new OtpService(redisTemplate, otpHasher, otpCodeGenerator, userRepository, eventPublisher, 10);
    }

    @Test
    void createSignupOtpStoresHashInRedisInsteadOfPlainOtp() {
        when(otpCodeGenerator.generate()).thenReturn("123456");
        when(otpHasher.hash("asha@example.com", "123456")).thenReturn("hashed-otp");

        String otp = otpService.createSignupOtp("ASHA@example.com");

        assertThat(otp).isEqualTo("123456");
        verify(valueOperations).set("otp:signup:asha@example.com", "hashed-otp", Duration.ofMinutes(10));
        verify(valueOperations, never()).set("otp:signup:asha@example.com", "123456", Duration.ofMinutes(10));
    }

    @Test
    void correctOtpMarksUserVerifiedDeletesRedisKeyAndPublishesEvent() {
        LibraryUser user = new LibraryUser(1L, "Asha", "asha@example.com", "encoded-password");
        when(userRepository.findByEmailIgnoreCase("asha@example.com")).thenReturn(Optional.of(user));
        when(valueOperations.get("otp:signup:asha@example.com")).thenReturn("hashed-otp");
        when(otpHasher.hash("asha@example.com", "123456")).thenReturn("hashed-otp");

        var response = otpService.verifySignupOtp(new VerifyOtpRequest("asha@example.com", "123456"));

        assertThat(response.emailVerified()).isTrue();
        assertThat(user.isEmailVerified()).isTrue();
        verify(redisTemplate).delete("otp:signup:asha@example.com");
        verify(eventPublisher).publishAfterCommit(
                eq(EventNames.USER_VERIFIED),
                eq("asha@example.com"),
                isA(UserVerifiedEvent.class)
        );
    }

    @Test
    void wrongOtpDoesNotVerifyUser() {
        LibraryUser user = new LibraryUser(1L, "Asha", "asha@example.com", "encoded-password");
        when(userRepository.findByEmailIgnoreCase("asha@example.com")).thenReturn(Optional.of(user));
        when(valueOperations.get("otp:signup:asha@example.com")).thenReturn("hashed-otp");
        when(otpHasher.hash("asha@example.com", "000000")).thenReturn("different-hash");

        assertThatThrownBy(() -> otpService.verifySignupOtp(new VerifyOtpRequest("asha@example.com", "000000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OTP is invalid");

        assertThat(user.isEmailVerified()).isFalse();
        verify(redisTemplate, never()).delete(any(String.class));
        verify(eventPublisher, never()).publishAfterCommit(any(), any(), any());
    }

    @Test
    void missingOtpReturnsExpiredOrMissingError() {
        LibraryUser user = new LibraryUser(1L, "Asha", "asha@example.com", "encoded-password");
        when(userRepository.findByEmailIgnoreCase("asha@example.com")).thenReturn(Optional.of(user));
        when(valueOperations.get("otp:signup:asha@example.com")).thenReturn(null);

        assertThatThrownBy(() -> otpService.verifySignupOtp(new VerifyOtpRequest("asha@example.com", "123456")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OTP is expired or missing");
    }

    @Test
    void alreadyVerifiedUserReturnsConflict() {
        LibraryUser user = new LibraryUser(1L, "Asha", "asha@example.com", "encoded-password");
        user.markEmailVerified();
        when(userRepository.findByEmailIgnoreCase("asha@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> otpService.verifySignupOtp(new VerifyOtpRequest("asha@example.com", "123456")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User email is already verified");
    }
}
