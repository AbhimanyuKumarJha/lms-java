package com.example.lms.books.controllers;

import com.example.lms.books.model.BookCopy;
import com.example.lms.books.model.BookCopyStatus;

import java.io.Serializable;

public record BookCopyResponse(
        Long bookCopyId,
        String title,
        String author,
        String description,
        String copyCode,
        BookCopyStatus status
) implements Serializable {

    public static BookCopyResponse from(BookCopy bookCopy) {
        return new BookCopyResponse(
                bookCopy.getBookCopyId(),
                bookCopy.getTitle(),
                bookCopy.getAuthor(),
                bookCopy.getDescription(),
                bookCopy.getCopyCode(),
                bookCopy.getStatus()
        );
    }
}
