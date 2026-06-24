package com.example.lms.notifications;

import com.example.lms.auth.events.SignupOtpRequestedEvent;
import com.example.lms.borrowing.events.BookBorrowedEvent;
import com.example.lms.borrowing.events.BookReturnedEvent;
import com.example.lms.fines.events.FineGeneratedEvent;
import com.example.lms.shared.events.EventNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    @KafkaListener(topics = EventNames.BOOK_BORROWED, groupId = "lms-notifications")
    public void onBookBorrowed(BookBorrowedEvent event) {
        log.info("Notification stub: borrowed transactionId={} userId={} bookCopyId={}",
                event.transactionId(), event.userId(), event.bookCopyId());
    }

    @KafkaListener(topics = EventNames.BOOK_RETURNED, groupId = "lms-notifications")
    public void onBookReturned(BookReturnedEvent event) {
        log.info("Notification stub: returned transactionId={} userId={} bookCopyId={}",
                event.transactionId(), event.userId(), event.bookCopyId());
    }

    @KafkaListener(topics = EventNames.FINE_GENERATED, groupId = "lms-notifications")
    public void onFineGenerated(FineGeneratedEvent event) {
        log.info("Notification stub: fine transactionId={} userId={} amount={}",
                event.transactionId(), event.userId(), event.amount());
    }

    @KafkaListener(topics = EventNames.SIGNUP_OTP_REQUESTED, groupId = "lms-notifications")
    public void onSignupOtpRequested(SignupOtpRequestedEvent event) {
        log.info("Signup OTP for email={} is {}", event.email(), event.otp());
    }
}
