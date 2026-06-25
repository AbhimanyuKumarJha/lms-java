package com.example.lms.reports;

import com.example.lms.books.events.BookAddedEvent;
import com.example.lms.borrowing.events.BookBorrowedEvent;
import com.example.lms.borrowing.events.BookReturnedEvent;
import com.example.lms.shared.events.EventNames;
import com.example.lms.users.events.UserCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReportProjectionListener {

    @KafkaListener(topics = EventNames.USER_CREATED, groupId = "lms-reports")
    public void onUserCreated(UserCreatedEvent event) {
    }

    @KafkaListener(topics = EventNames.BOOK_ADDED, groupId = "lms-reports")
    public void onBookAdded(BookAddedEvent event) {
    }

    @KafkaListener(topics = EventNames.BOOK_BORROWED, groupId = "lms-reports")
    public void onBookBorrowed(BookBorrowedEvent event) {
    }

    @KafkaListener(topics = EventNames.BOOK_RETURNED, groupId = "lms-reports")
    public void onBookReturned(BookReturnedEvent event) {
    }
}
