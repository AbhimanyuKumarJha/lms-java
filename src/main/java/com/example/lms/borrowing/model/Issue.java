package com.example.lms.borrowing.model;

import com.example.lms.books.model.BookCopy;
import com.example.lms.users.model.LibraryUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "issues")
public class Issue {

    @Id
    private Long transactionId;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private LibraryUser user;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "book_copy_id", nullable = false)
    private BookCopy bookCopy;
    @Column(nullable = false)
    private LocalDate issuedAt;
    @Column(nullable = false)
    private LocalDate dueDate;
    private LocalDate returnedAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueStatus status;

    public Issue() {
    }

    public Issue(Long transactionId, LibraryUser user, BookCopy bookCopy, LocalDate issuedAt, LocalDate dueDate) {
        this.transactionId = transactionId;
        this.user = user;
        this.bookCopy = bookCopy;
        this.issuedAt = issuedAt;
        this.dueDate = dueDate;
        this.status = IssueStatus.ACTIVE;
    }

    public boolean isOverdue() {
        return status == IssueStatus.ACTIVE && LocalDate.now().isAfter(dueDate);
    }

    public void returnBook() {
        if (status != IssueStatus.ACTIVE) {
            throw new IllegalStateException("This issue is not active");
        }

        this.returnedAt = LocalDate.now();
        this.status = IssueStatus.RETURNED;
        this.bookCopy.markAvailable();
    }

    public BigDecimal calculateFine(BigDecimal finePerDay) {
        LocalDate effectiveReturnDate = returnedAt != null ? returnedAt : LocalDate.now();

        if (!effectiveReturnDate.isAfter(dueDate)) {
            return BigDecimal.ZERO;
        }

        long overdueDays = ChronoUnit.DAYS.between(dueDate, effectiveReturnDate);
        return finePerDay.multiply(BigDecimal.valueOf(overdueDays));
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public LibraryUser getUser() {
        return user;
    }

    public void setUser(LibraryUser user) {
        this.user = user;
    }

    public BookCopy getBookCopy() {
        return bookCopy;
    }

    public void setBookCopy(BookCopy bookCopy) {
        this.bookCopy = bookCopy;
    }

    public LocalDate getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDate issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(LocalDate returnedAt) {
        this.returnedAt = returnedAt;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public void setStatus(IssueStatus status) {
        this.status = status;
    }
}
