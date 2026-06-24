package com.example.lms.reports;

import com.example.lms.books.events.BookAddedEvent;
import com.example.lms.borrowing.events.BookBorrowedEvent;
import com.example.lms.borrowing.events.BookReturnedEvent;
import com.example.lms.shared.events.EventNames;
import com.example.lms.users.events.UserCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReportProjectionListener {

    private static final Logger log = LoggerFactory.getLogger(ReportProjectionListener.class);

    @KafkaListener(topics = EventNames.USER_CREATED, groupId = "lms-reports")
    public void onUserCreated(UserCreatedEvent event) {
        log.info("Report projection stub: user-created userId={}", event.userId());
    }

    @KafkaListener(topics = EventNames.BOOK_ADDED, groupId = "lms-reports")
    public void onBookAdded(BookAddedEvent event) {
        log.info("Report projection stub: book-added bookId={}", event.bookId());
    }

    @KafkaListener(topics = EventNames.BOOK_BORROWED, groupId = "lms-reports")
    public void onBookBorrowed(BookBorrowedEvent event) {
        log.info("Report projection stub: book-borrowed transactionId={}", event.transactionId());
    }

    @KafkaListener(topics = EventNames.BOOK_RETURNED, groupId = "lms-reports")
    public void onBookReturned(BookReturnedEvent event) {
        log.info("Report projection stub: book-returned transactionId={}", event.transactionId());
    }
}
