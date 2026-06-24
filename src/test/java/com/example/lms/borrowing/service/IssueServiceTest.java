package com.example.lms.borrowing.service;

import com.example.lms.books.model.Book;
import com.example.lms.books.model.BookCopy;
import com.example.lms.books.model.BookCopyStatus;
import com.example.lms.books.repository.BookCopyRepository;
import com.example.lms.borrowing.model.Issue;
import com.example.lms.borrowing.model.IssueStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IssueServiceTest {

    private IssueRepository issueRepository;
    private LibraryUserRepository userRepository;
    private BookCopyRepository bookCopyRepository;
    private DomainEventPublisher eventPublisher;
    private IssueService issueService;

    @BeforeEach
    void setUp() {
        issueRepository = mock(IssueRepository.class);
        userRepository = mock(LibraryUserRepository.class);
        bookCopyRepository = mock(BookCopyRepository.class);
        eventPublisher = mock(DomainEventPublisher.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<IssueResponse> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        issueService = new IssueService(
                issueRepository,
                userRepository,
                bookCopyRepository,
                transactionTemplate,
                new FineService(),
                eventPublisher
        );
    }

    @Test
    void borrowingAvailableCopyCreatesActiveIssueAndPublishesEvent() {
        LibraryUser user = user();
        BookCopy copy = new BookCopy(10L, new Book(5L, "Clean Code", "Robert Martin", null), "CC-1");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookCopyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(copy));
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IssueResponse response = issueService.createIssue(new CreateIssueRequest(100L, 1L, 10L));

        assertThat(response.transactionId()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(IssueStatus.ACTIVE);
        assertThat(copy.getStatus()).isEqualTo(BookCopyStatus.ISSUED);
        verify(eventPublisher).publishAfterCommit(
                eq(EventNames.BOOK_BORROWED),
                eq("100"),
                isA(BookBorrowedEvent.class)
        );
    }

    @Test
    void borrowingUnavailableCopyFailsWithoutPublishingEvent() {
        LibraryUser user = user();
        BookCopy copy = new BookCopy(10L, new Book(5L, "Clean Code", "Robert Martin", null), "CC-1");
        copy.markIssued();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookCopyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(copy));

        assertThatThrownBy(() -> issueService.createIssue(new CreateIssueRequest(100L, 1L, 10L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Book copy is not available");

        verify(issueRepository, never()).save(any(Issue.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void returningBookMarksIssueReturnedAndPublishesReturnEvent() {
        Issue issue = activeIssue(LocalDate.now().plusDays(1));
        when(issueRepository.findById(100L)).thenReturn(Optional.of(issue));

        IssueResponse response = issueService.returnBook(100L);

        assertThat(response.status()).isEqualTo(IssueStatus.RETURNED);
        assertThat(issue.getBookCopy().getStatus()).isEqualTo(BookCopyStatus.AVAILABLE);
        verify(eventPublisher).publishAfterCommit(
                eq(EventNames.BOOK_RETURNED),
                eq("100"),
                isA(BookReturnedEvent.class)
        );
        verify(eventPublisher, never()).publishAfterCommit(
                eq(EventNames.FINE_GENERATED),
                eq("100"),
                any(FineGeneratedEvent.class)
        );
    }

    @Test
    void returningOverdueBookPublishesFineGeneratedEvent() {
        Issue issue = activeIssue(LocalDate.now().minusDays(2));
        when(issueRepository.findById(100L)).thenReturn(Optional.of(issue));

        issueService.returnBook(100L);

        verify(eventPublisher).publishAfterCommit(
                eq(EventNames.BOOK_RETURNED),
                eq("100"),
                isA(BookReturnedEvent.class)
        );
        verify(eventPublisher).publishAfterCommit(
                eq(EventNames.FINE_GENERATED),
                eq("100"),
                isA(FineGeneratedEvent.class)
        );
    }

    private Issue activeIssue(LocalDate dueDate) {
        BookCopy copy = new BookCopy(10L, new Book(5L, "Clean Code", "Robert Martin", null), "CC-1");
        copy.markIssued();
        return new Issue(100L, user(), copy, LocalDate.now().minusDays(14), dueDate);
    }

    private LibraryUser user() {
        return new LibraryUser(1L, "Asha", "asha@example.com", "encoded-password");
    }
}
