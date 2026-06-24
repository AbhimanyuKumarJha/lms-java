package com.example.lms.borrowing.service;

import com.example.lms.books.model.BookCopy;
import com.example.lms.books.repository.BookCopyRepository;
import com.example.lms.borrowing.model.Issue;
import com.example.lms.borrowing.events.BookBorrowedEvent;
import com.example.lms.borrowing.events.BookReturnedEvent;
import com.example.lms.borrowing.repository.IssueRepository;
import com.example.lms.borrowing.controllers.CreateIssueRequest;
import com.example.lms.borrowing.controllers.IssueResponse;
import com.example.lms.fines.events.FineGeneratedEvent;
import com.example.lms.fines.service.FineService;
import com.example.lms.shared.events.DomainEventPublisher;
import com.example.lms.shared.events.EventNames;
import com.example.lms.users.model.LibraryUser;
import com.example.lms.users.repository.LibraryUserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final LibraryUserRepository userRepository;
    private final BookCopyRepository bookCopyRepository;
    private final TransactionTemplate transactionTemplate;
    private final FineService fineService;
    private final DomainEventPublisher eventPublisher;

    public IssueService(
            IssueRepository issueRepository,
            LibraryUserRepository userRepository,
            BookCopyRepository bookCopyRepository,
            TransactionTemplate transactionTemplate,
            FineService fineService,
            DomainEventPublisher eventPublisher
    ) {
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.transactionTemplate = transactionTemplate;
        this.fineService = fineService;
        this.eventPublisher = eventPublisher;
    }

    @CacheEvict(cacheNames = {"book-copies", "book-copy-counts", "user-profiles"}, allEntries = true)
    public IssueResponse createIssue(CreateIssueRequest request) {
        return transactionTemplate.execute(status -> IssueResponse.from(createIssueInTransaction(request)));
    }

    private Issue createIssueInTransaction(CreateIssueRequest request) {
        LibraryUser user = findUser(request.userId());
        BookCopy bookCopy = findBookCopy(request.bookCopyId());

        if (!bookCopy.isAvailable()) {
            throw new IllegalStateException("Book copy is not available");
        }

        Issue issue = new Issue(
                request.transactionId(),
                user,
                bookCopy,
                LocalDate.now(),
                LocalDate.now().plusDays(14)
        );

        bookCopy.markIssued();
        Issue savedIssue = issueRepository.save(issue);

        eventPublisher.publishAfterCommit(
                EventNames.BOOK_BORROWED,
                String.valueOf(savedIssue.getTransactionId()),
                new BookBorrowedEvent(
                        savedIssue.getTransactionId(),
                        user.getUserId(),
                        bookCopy.getBookCopyId(),
                        bookCopy.getBook().getBookId(),
                        savedIssue.getIssuedAt(),
                        savedIssue.getDueDate(),
                        Instant.now()
                )
        );

        return savedIssue;
    }

    public List<IssueResponse> createIssues(List<CreateIssueRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }

        int threadCount = Math.min(requests.size(), Runtime.getRuntime().availableProcessors());
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        try {
            List<CompletableFuture<IssueResponse>> futures = requests.stream()
                    .map(request -> CompletableFuture.supplyAsync(() -> createIssue(request), executorService))
                    .toList();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
        } finally {
            executorService.shutdown();
        }
    }

    @Transactional
    @CacheEvict(cacheNames = {"book-copies", "book-copy-counts", "user-profiles"}, allEntries = true)
    public IssueResponse returnBook(Long transactionId) {
        Issue issue = findIssue(transactionId);
        issue.returnBook();

        publishReturnEvents(issue);
        return IssueResponse.from(issue);
    }

    public List<IssueResponse> getAllIssues() {
        return issueRepository.findAll().stream()
                .map(IssueResponse::from)
                .toList();
    }

    private void publishReturnEvents(Issue issue) {
        eventPublisher.publishAfterCommit(
                EventNames.BOOK_RETURNED,
                String.valueOf(issue.getTransactionId()),
                new BookReturnedEvent(
                        issue.getTransactionId(),
                        issue.getUser().getUserId(),
                        issue.getBookCopy().getBookCopyId(),
                        issue.getBookCopy().getBook().getBookId(),
                        issue.getReturnedAt(),
                        Instant.now()
                )
        );

        BigDecimal fine = fineService.calculateFine(issue);
        if (fine.compareTo(BigDecimal.ZERO) > 0) {
            eventPublisher.publishAfterCommit(
                    EventNames.FINE_GENERATED,
                    String.valueOf(issue.getTransactionId()),
                    new FineGeneratedEvent(
                            issue.getTransactionId(),
                            issue.getUser().getUserId(),
                            fine,
                            issue.getDueDate(),
                            issue.getReturnedAt(),
                            Instant.now()
                    )
            );
        }
    }

    private BookCopy findBookCopy(Long bookCopyId) {
        return bookCopyRepository.findByIdForUpdate(bookCopyId)
                .orElseThrow(() -> new IllegalArgumentException("Book copy not found"));
    }

    private LibraryUser findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private Issue findIssue(Long transactionId) {
        return issueRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Issue transaction not found"));
    }
}
