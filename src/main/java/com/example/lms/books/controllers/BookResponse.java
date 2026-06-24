package com.example.lms.books.controllers;

import com.example.lms.books.model.Book;

import java.io.Serializable;

public record BookResponse(
        Long bookId,
        String title,
        String author,
        String description
) implements Serializable {

    public static BookResponse from(Book book) {
        return new BookResponse(book.getBookId(), book.getTitle(), book.getAuthor(), book.getDescription());
    }
}
