package com.example.lms.books.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "book_copies")
public class BookCopy {

    @Id
    private Long bookCopyId;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private String author;
    @Column(length = 1000)
    private String description;
    @Column(nullable = false, unique = true)
    private String copyCode;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookCopyStatus status;

    public BookCopy() {
        this.status = BookCopyStatus.AVAILABLE;
    }

    public BookCopy(Long bookCopyId, String title, String author, String description, String copyCode) {
        this.bookCopyId = bookCopyId;
        this.title = title;
        this.author = author;
        this.description = description;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
