package com.example.lms.borrowing.controllers;

import com.example.lms.borrowing.model.Issue;
import com.example.lms.borrowing.model.IssueStatus;

import java.io.Serializable;
import java.time.LocalDate;

public record IssueResponse(
        Long transactionId,
        Long userId,
        String userName,
        Long bookCopyId,
        Long bookId,
        String bookTitle,
        String copyCode,
        LocalDate issuedAt,
        LocalDate dueDate,
        LocalDate returnedAt,
        IssueStatus status
) implements Serializable {

    public static IssueResponse from(Issue issue) {
        return new IssueResponse(
                issue.getTransactionId(),
                issue.getUser().getUserId(),
                issue.getUser().getName(),
                issue.getBookCopy().getBookCopyId(),
                issue.getBookCopy().getBook().getBookId(),
                issue.getBookCopy().getBook().getTitle(),
                issue.getBookCopy().getCopyCode(),
                issue.getIssuedAt(),
                issue.getDueDate(),
                issue.getReturnedAt(),
                issue.getStatus()
        );
    }
}
