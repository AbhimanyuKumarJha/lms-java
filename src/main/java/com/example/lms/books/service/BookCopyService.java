package com.example.lms.books.service;

import com.example.lms.books.model.Book;
import com.example.lms.books.model.BookCopy;
import com.example.lms.books.model.BookCopyStatus;
import com.example.lms.books.repository.BookCopyRepository;
import com.example.lms.books.repository.BookRepository;
import com.example.lms.books.controllers.BookCopyResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookCopyService {

    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;

    public BookCopyService(BookCopyRepository bookCopyRepository, BookRepository bookRepository) {
        this.bookCopyRepository = bookCopyRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional
    @CacheEvict(cacheNames = {"book-copies", "book-copy-counts"}, allEntries = true)
    public BookCopyResponse createBookCopy(Long bookCopyId, Long bookId, String copyCode) {
        Book book = findBook(bookId);
        BookCopy bookCopy = new BookCopy(bookCopyId, book, copyCode);
        return BookCopyResponse.from(bookCopyRepository.save(bookCopy));
    }

    @Cacheable(cacheNames = "book-copies", key = "'all'")
    public List<BookCopyResponse> getAllBookCopies() {
        return bookCopyRepository.findAll().stream()
                .map(BookCopyResponse::from)
                .toList();
    }

    @Cacheable(cacheNames = "book-copy-counts", key = "'total:' + #bookId")
    public long getTotalCopies(Long bookId) {
        return bookCopyRepository.countByBook_BookId(bookId);
    }

    @Cacheable(cacheNames = "book-copy-counts", key = "'available:' + #bookId")
    public long getAvailableCopies(Long bookId) {
        return bookCopyRepository.countByBook_BookIdAndStatus(bookId, BookCopyStatus.AVAILABLE);
    }

    private Book findBook(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));
    }
}
