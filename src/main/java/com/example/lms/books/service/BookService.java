package com.example.lms.books.service;

import com.example.lms.books.model.Book;
import com.example.lms.books.events.BookAddedEvent;
import com.example.lms.books.repository.BookRepository;
import com.example.lms.books.controllers.BookResponse;
import com.example.lms.books.controllers.CreateBookRequest;
import com.example.lms.shared.events.DomainEventPublisher;
import com.example.lms.shared.events.EventNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final DomainEventPublisher eventPublisher;

    public BookService(BookRepository bookRepository, DomainEventPublisher eventPublisher) {
        this.bookRepository = bookRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @CacheEvict(cacheNames = {"books", "book-copies"}, allEntries = true)
    public BookResponse createBook(CreateBookRequest request) {
        Book book = bookRepository.save(new Book(
                request.bookId(),
                request.title(),
                request.author(),
                request.description()
        ));

        eventPublisher.publishAfterCommit(
                EventNames.BOOK_ADDED,
                String.valueOf(book.getBookId()),
                new BookAddedEvent(book.getBookId(), book.getTitle(), book.getAuthor(), Instant.now())
        );

        return BookResponse.from(book);
    }

    @Cacheable(cacheNames = "books", key = "'all'")
    public List<BookResponse> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(BookResponse::from)
                .toList();
    }
}
