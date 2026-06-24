package com.example.lms.books.controllers;

import com.example.lms.books.model.BookCopy;
import com.example.lms.books.model.BookCopyStatus;

import java.io.Serializable;

public record BookCopyResponse(
        Long bookCopyId,
        Long bookId,
        String title,
        String copyCode,
        BookCopyStatus status
) implements Serializable {

    public static BookCopyResponse from(BookCopy bookCopy) {
        return new BookCopyResponse(
                bookCopy.getBookCopyId(),
                bookCopy.getBook().getBookId(),
                bookCopy.getBook().getTitle(),
                bookCopy.getCopyCode(),
                bookCopy.getStatus()
        );
    }
}
