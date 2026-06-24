package com.example.lms.books.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "book_copies")
public class BookCopy {

    @Id
    private Long bookCopyId;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;
    @Column(nullable = false, unique = true)
    private String copyCode;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookCopyStatus status;

    public BookCopy() {
        this.status = BookCopyStatus.AVAILABLE;
    }

    public BookCopy(Long bookCopyId, Book book, String copyCode) {
        this.bookCopyId = bookCopyId;
        this.book = book;
        this.copyCode = copyCode;
        this.status = BookCopyStatus.AVAILABLE;
    }

    public boolean isAvailable() {
        return status == BookCopyStatus.AVAILABLE;
    }

    public void markIssued() {
        this.status = BookCopyStatus.ISSUED;
    }

    public void markAvailable() {
        this.status = BookCopyStatus.AVAILABLE;
    }

    public Long getBookCopyId() {
        return bookCopyId;
    }

    public void setBookCopyId(Long bookCopyId) {
        this.bookCopyId = bookCopyId;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public String getCopyCode() {
        return copyCode;
    }

    public void setCopyCode(String copyCode) {
        this.copyCode = copyCode;
    }

    public BookCopyStatus getStatus() {
        return status;
    }

    public void setStatus(BookCopyStatus status) {
        this.status = status;
    }
}
