package com.example.lms.auth.service;

import com.example.lms.auth.controllers.SignupRequest;
import com.example.lms.auth.events.SignupOtpRequestedEvent;
import com.example.lms.otp.service.OtpService;
import com.example.lms.shared.events.DomainEventPublisher;
import com.example.lms.shared.events.EventNames;
import com.example.lms.users.model.LibraryUser;
import com.example.lms.users.repository.LibraryUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void signupCreatesUnverifiedUserStoresOtpAndPublishesOtpEvent() {
        LibraryUserRepository userRepository = mock(LibraryUserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        OtpService otpService = mock(OtpService.class);
        DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
        AuthService authService = new AuthService(userRepository, passwordEncoder, otpService, eventPublisher);

        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(otpService.createSignupOtp("asha@example.com")).thenReturn("123456");
        when(userRepository.save(any(LibraryUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.signup(new SignupRequest("Asha", "ASHA@example.com", "password123"));

        assertThat(response.email()).isEqualTo("asha@example.com");
        assertThat(response.emailVerified()).isFalse();
        verify(otpService).createSignupOtp("asha@example.com");
        verify(eventPublisher).publishAfterCommit(
                eq(EventNames.SIGNUP_OTP_REQUESTED),
                eq("asha@example.com"),
                isA(SignupOtpRequestedEvent.class)
        );
    }

    @Test
    void signupRejectsDuplicateEmail() {
        LibraryUserRepository userRepository = mock(LibraryUserRepository.class);
        AuthService authService = new AuthService(
                userRepository,
                mock(PasswordEncoder.class),
                mock(OtpService.class),
                mock(DomainEventPublisher.class)
        );

        when(userRepository.existsByEmailIgnoreCase("asha@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(new SignupRequest("Asha", "asha@example.com", "password123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email is already registered");

        verify(userRepository, never()).save(any(LibraryUser.class));
    }
}
