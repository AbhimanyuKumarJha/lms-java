package com.example.lms.shared.events;

public final class EventNames {

    public static final String USER_CREATED = "user-created";
    public static final String BOOK_ADDED = "book-added";
    public static final String BOOK_BORROWED = "book-borrowed";
    public static final String BOOK_RETURNED = "book-returned";
    public static final String FINE_GENERATED = "fine-generated";
    public static final String SIGNUP_OTP_REQUESTED = "signup-otp-requested";
    public static final String USER_VERIFIED = "user-verified";

    private EventNames() {
    }
}
