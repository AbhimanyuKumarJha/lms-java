package com.example.lms.users.service;

import com.example.lms.borrowing.model.Issue;
import com.example.lms.borrowing.model.IssueStatus;
import com.example.lms.borrowing.repository.IssueRepository;
import com.example.lms.borrowing.controllers.IssueResponse;
import com.example.lms.fines.service.FineService;
import com.example.lms.shared.events.DomainEventPublisher;
import com.example.lms.shared.events.EventNames;
import com.example.lms.users.model.LibraryUser;
import com.example.lms.users.events.UserCreatedEvent;
import com.example.lms.users.repository.LibraryUserRepository;
import com.example.lms.users.controllers.CreateUserRequest;
import com.example.lms.users.controllers.UserResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class UserService {

    private final LibraryUserRepository userRepository;
    private final IssueRepository issueRepository;
    private final FineService fineService;
    private final DomainEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            LibraryUserRepository userRepository,
            IssueRepository issueRepository,
            FineService fineService,
            DomainEventPublisher eventPublisher,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.issueRepository = issueRepository;
        this.fineService = fineService;
        this.eventPublisher = eventPublisher;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @CacheEvict(cacheNames = {"users", "user-profiles"}, allEntries = true)
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        LibraryUser user = userRepository.save(new LibraryUser(
                request.userId(),
                request.name(),
                request.email().toLowerCase(),
                passwordEncoder.encode(request.password())
        ));

        eventPublisher.publishAfterCommit(
                EventNames.USER_CREATED,
                String.valueOf(user.getUserId()),
                new UserCreatedEvent(user.getUserId(), user.getEmail(), user.getName(), Instant.now())
        );

        return UserResponse.from(user);
    }

    @Cacheable(cacheNames = "users", key = "'all'")
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Cacheable(cacheNames = "user-profiles", key = "#userId")
    public UserResponse getUser(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public List<IssueResponse> getIssuedBooks(Long userId) {
        return issueRepository.findByUser_UserIdAndStatus(userId, IssueStatus.ACTIVE).stream()
                .map(IssueResponse::from)
                .toList();
    }

    public BigDecimal getTotalFine(Long userId) {
        return issueRepository.findByUser_UserId(userId).stream()
                .map(fineService::calculateFine)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
