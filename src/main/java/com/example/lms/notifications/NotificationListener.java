package com.example.lms.notifications;

import com.example.lms.auth.events.SignupOtpRequestedEvent;
import com.example.lms.borrowing.events.BookBorrowedEvent;
import com.example.lms.borrowing.events.BookReturnedEvent;
import com.example.lms.fines.events.FineGeneratedEvent;
import com.example.lms.shared.events.EventNames;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    @KafkaListener(topics = EventNames.BOOK_BORROWED, groupId = "lms-notifications")
    public void onBookBorrowed(BookBorrowedEvent event) {
    }

    @KafkaListener(topics = EventNames.BOOK_RETURNED, groupId = "lms-notifications")
    public void onBookReturned(BookReturnedEvent event) {
    }

    @KafkaListener(topics = EventNames.FINE_GENERATED, groupId = "lms-notifications")
    public void onFineGenerated(FineGeneratedEvent event) {
    }

    @KafkaListener(topics = EventNames.SIGNUP_OTP_REQUESTED, groupId = "lms-notifications")
    public void onSignupOtpRequested(SignupOtpRequestedEvent event) {
    }
}
