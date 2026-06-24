package com.example.lms.users.service;

import com.example.lms.borrowing.repository.IssueRepository;
import com.example.lms.fines.service.FineService;
import com.example.lms.shared.events.DomainEventPublisher;
import com.example.lms.shared.events.EventNames;
import com.example.lms.users.model.LibraryUser;
import com.example.lms.users.events.UserCreatedEvent;
import com.example.lms.users.repository.LibraryUserRepository;
import com.example.lms.users.controllers.CreateUserRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Test
    void creatingUserPublishesUserCreatedEvent() {
        LibraryUserRepository userRepository = mock(LibraryUserRepository.class);
        IssueRepository issueRepository = mock(IssueRepository.class);
        DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UserService userService = new UserService(
                userRepository,
                issueRepository,
                new FineService(),
                eventPublisher,
                passwordEncoder
        );

        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(LibraryUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = userService.createUser(new CreateUserRequest(
                1L,
                "Asha",
                "asha@example.com",
                "password123"
        ));

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("asha@example.com");
        verify(eventPublisher).publishAfterCommit(
                eq(EventNames.USER_CREATED),
                eq("1"),
                isA(UserCreatedEvent.class)
        );
    }
}
