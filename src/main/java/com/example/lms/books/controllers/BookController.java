package com.example.lms.books.controllers;

import com.example.lms.books.service.BookCopyService;
import com.example.lms.books.service.BookService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;
    private final BookCopyService bookCopyService;

    public BookController(BookService bookService, BookCopyService bookCopyService) {
        this.bookService = bookService;
        this.bookCopyService = bookCopyService;
    }

    @PostMapping
    public BookResponse createBook(@Valid @RequestBody CreateBookRequest request) {
        return bookService.createBook(request);
    }

    @GetMapping
    public List<BookResponse> getBooks() {
        return bookService.getAllBooks();
    }

    @GetMapping("/{bookId}/copies/total")
    public long getTotalCopies(@PathVariable Long bookId) {
        return bookCopyService.getTotalCopies(bookId);
    }

    @GetMapping("/{bookId}/copies/available")
    public long getAvailableCopies(@PathVariable Long bookId) {
        return bookCopyService.getAvailableCopies(bookId);
    }
}
