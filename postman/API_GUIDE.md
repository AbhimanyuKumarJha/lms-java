# LMS API Guide

Base URL:

```text
http://localhost:8080
```

Import these into Postman:

```text
postman/lms-api.postman_collection.json
postman/lms-local.postman_environment.json
```

Recommended flow:

```text
1. Start Docker services: PostgreSQL, Redis, Kafka
2. Start Spring Boot app
3. Signup user
4. Copy OTP from Spring Boot terminal logs
5. Verify OTP
6. Create book
7. Create book copy
8. Borrow book
9. Return book
```

## Auth APIs

### Signup

Creates an unverified user, hashes the password, generates a 6-digit OTP, stores the hashed OTP in Redis, and publishes a Kafka event. The notification listener prints the plain OTP in the Spring Boot terminal.

```http
POST /api/auth/signup
```

Payload:

```json
{
  "name": "Asha Sharma",
  "email": "asha@example.com",
  "password": "password123"
}
```

What to do:

After sending this request, check your Spring Boot terminal for a log like:

```text
Signup OTP for email=asha@example.com is 123456
```

Copy that OTP into the Postman environment variable named `otp`.

### Verify OTP

Verifies the signup OTP. If correct, the user becomes email verified and the OTP key is deleted from Redis.

```http
POST /api/otp/verify
```

Payload:

```json
{
  "email": "asha@example.com",
  "otp": "123456"
}
```

What to do:

Use the OTP printed in the Spring Boot terminal. Do not use the example value unless it matches your actual terminal OTP.

## User APIs

### Create User

Creates a library user directly. This is more of an internal/admin-style endpoint. For public user registration, prefer `POST /api/auth/signup`.

```http
POST /api/users
```

Payload:

```json
{
  "userId": 1000000001,
  "name": "Asha Sharma",
  "email": "asha@example.com",
  "password": "password123"
}
```

What to do:

Use this only when you want to manually control `userId`. The password is hashed before storage.

### Get Users

Returns all users.

```http
GET /api/users
```

Payload:

```text
No request body.
```

### Get User Issued Books

Returns active borrowed books for a user.

```http
GET /api/users/{userId}/issued-books
```

Example:

```http
GET /api/users/1000000001/issued-books
```

Payload:

```text
No request body.
```

### Get User Fine

Returns the total calculated fine for a user.

```http
GET /api/users/{userId}/fine
```

Example:

```http
GET /api/users/1000000001/fine
```

Payload:

```text
No request body.
```

## Book APIs

### Create Book

Creates a book record.

```http
POST /api/books
```

Payload:

```json
{
  "bookId": 101,
  "title": "Clean Architecture",
  "author": "Robert C. Martin",
  "description": "A practical guide to software architecture principles."
}
```

What to do:

Create a book before creating a book copy.

### Get Books

Returns all books. This response is Redis-cacheable in the app.

```http
GET /api/books
```

Payload:

```text
No request body.
```

### Get Total Copies

Returns total copies for a book.

```http
GET /api/books/{bookId}/copies/total
```

Example:

```http
GET /api/books/101/copies/total
```

Payload:

```text
No request body.
```

### Get Available Copies

Returns available copies for a book.

```http
GET /api/books/{bookId}/copies/available
```

Example:

```http
GET /api/books/101/copies/available
```

Payload:

```text
No request body.
```

## Book Copy APIs

### Create Book Copy

Creates a physical/library copy of an existing book.

```http
POST /api/book-copies
```

Payload:

```json
{
  "bookCopyId": 1001,
  "bookId": 101,
  "copyCode": "BK-101-COPY-1"
}
```

What to do:

Create the book first. Then create a copy for that `bookId`.

### Get Book Copies

Returns all book copies.

```http
GET /api/book-copies
```

Payload:

```text
No request body.
```

## Borrowing APIs

### Borrow Book

Creates an issue transaction. The app locks the book copy, verifies it is available, marks it issued, saves the transaction, and publishes a Kafka `book-borrowed` event after commit.

```http
POST /api/issues
```

Payload:

```json
{
  "transactionId": 5001,
  "userId": 1000000001,
  "bookCopyId": 1001
}
```

What to do:

Before this request:

```text
1. Create or signup a user.
2. Create a book.
3. Create a book copy.
```

### Borrow Books In Bulk

Creates multiple issue transactions.

```http
POST /api/issues/bulk
```

Payload:

```json
[
  {
    "transactionId": 5001,
    "userId": 1000000001,
    "bookCopyId": 1001
  }
]
```

What to do:

Use different `transactionId` and `bookCopyId` values for each item.

### Return Book

Returns a borrowed book. The app marks the issue returned, makes the copy available again, publishes `book-returned`, and publishes `fine-generated` if a fine exists.

```http
POST /api/issues/{transactionId}/return
```

Example:

```http
POST /api/issues/5001/return
```

Payload:

```text
No request body.
```

### Get Issues

Returns all borrowing issue transactions.

```http
GET /api/issues
```

Payload:

```text
No request body.
```

## Common Problems

### Duplicate Email

If signup or create user returns an error about email already registered, change the email in Postman:

```text
asha2@example.com
```

### Duplicate IDs

If book, copy, issue, or user creation fails because an ID already exists, change the related Postman environment variable:

```text
userId
bookId
bookCopyId
transactionId
```

### OTP Invalid Or Missing

The OTP expires after 10 minutes. Run signup again with a new email, then copy the new OTP from terminal logs.

### Kafka Or Redis Errors

Make sure Docker services are running:

```powershell
docker compose up -d postgres redis kafka
```

