package com.example.lms.books.controllers;

import com.example.lms.books.service.BookCopyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/book-copies")
public class BookCopyController {

    private final BookCopyService bookCopyService;

    public BookCopyController(BookCopyService bookCopyService) {
        this.bookCopyService = bookCopyService;
    }

    @PostMapping
    public BookCopyResponse createBookCopy(@Valid @RequestBody CreateBookCopyRequest request) {
        return bookCopyService.createBookCopy(request.bookCopyId(), request.bookId(), request.copyCode());
    }

    @GetMapping
    public List<BookCopyResponse> getBookCopies() {
        return bookCopyService.getAllBookCopies();
    }
}
