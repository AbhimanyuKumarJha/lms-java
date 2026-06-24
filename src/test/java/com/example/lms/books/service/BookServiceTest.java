package com.example.lms.books.service;

import com.example.lms.books.model.Book;
import com.example.lms.books.events.BookAddedEvent;
import com.example.lms.books.repository.BookRepository;
import com.example.lms.books.controllers.CreateBookRequest;
import com.example.lms.shared.events.DomainEventPublisher;
import com.example.lms.shared.events.EventNames;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookServiceTest {

    @Test
    void creatingBookPublishesBookAddedEvent() {
        BookRepository bookRepository = mock(BookRepository.class);
        DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
        BookService bookService = new BookService(bookRepository, eventPublisher);

        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = bookService.createBook(new CreateBookRequest(5L, "Clean Code", "Robert Martin", null));

        assertThat(response.bookId()).isEqualTo(5L);
        verify(eventPublisher).publishAfterCommit(
                eq(EventNames.BOOK_ADDED),
                eq("5"),
                isA(BookAddedEvent.class)
        );
    }
}
