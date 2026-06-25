package com.example.lms.books.service;

import com.example.lms.books.controllers.BookCopyResponse;
import com.example.lms.books.model.BookCopy;
import com.example.lms.books.model.BookCopyStatus;
import com.example.lms.books.repository.BookCopyRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookCopyService {

    private final BookCopyRepository bookCopyRepository;

    public BookCopyService(BookCopyRepository bookCopyRepository) {
        this.bookCopyRepository = bookCopyRepository;
    }

    @Transactional
    @CacheEvict(cacheNames = {"book-copies", "book-copy-counts"}, allEntries = true)
    public BookCopyResponse createBookCopy(
            Long bookCopyId,
            String title,
            String author,
            String description,
            String copyCode
    ) {
        BookCopy bookCopy = new BookCopy(bookCopyId, title, author, description, copyCode);
        return BookCopyResponse.from(bookCopyRepository.save(bookCopy));
    }

    @Cacheable(cacheNames = "book-copies", key = "'all'")
    public List<BookCopyResponse> getAllBookCopies() {
        return bookCopyRepository.findAll().stream()
                .map(BookCopyResponse::from)
                .toList();
    }

    @Cacheable(cacheNames = "book-copy-counts", key = "'total:' + #title")
    public long getTotalCopies(String title) {
        return bookCopyRepository.countByTitle(title);
    }

    @Cacheable(cacheNames = "book-copy-counts", key = "'available:' + #title")
    public long getAvailableCopies(String title) {
        return bookCopyRepository.countByTitleAndStatus(title, BookCopyStatus.AVAILABLE);
    }
}
