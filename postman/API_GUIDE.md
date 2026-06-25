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
4. Deliver or inspect the generated signup OTP through your configured notification path
5. Verify OTP
6. Create book copy
7. Borrow book
8. Return book
```

## Auth APIs

### Signup

Creates an unverified user, hashes the password, generates a 6-digit OTP, stores the hashed OTP in Redis, and publishes a Kafka event. Notification delivery is currently stubbed.

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

Use your configured notification path to get the generated OTP, then copy it into the Postman environment variable named `otp`.

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

Use the generated OTP. Do not use the example value unless it matches the actual OTP for that email.

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

## Book Copy APIs

### Create Book Copy

Creates a physical/library copy with the book details stored directly on the copy.

```http
POST /api/book-copies
```

Payload:

```json
{
  "bookCopyId": 1001,
  "title": "Clean Architecture",
  "author": "Robert C. Martin",
  "description": "A practical guide to software architecture principles.",
  "copyCode": "BK-101-COPY-1"
}
```

### Get Book Copies

Returns all book copies.

```http
GET /api/book-copies
```

Payload:

```text
No request body.
```

### Get Total Copies

Returns total copies for a title.

```http
GET /api/book-copies/titles/{title}/copies/total
```

Example:

```http
GET /api/book-copies/titles/Clean%20Architecture/copies/total
```

Payload:

```text
No request body.
```

### Get Available Copies

Returns available copies for a title.

```http
GET /api/book-copies/titles/{title}/copies/available
```

Example:

```http
GET /api/book-copies/titles/Clean%20Architecture/copies/available
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
2. Create a book copy.
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

If copy, issue, or user creation fails because an ID already exists, change the related Postman environment variable:

```text
userId
bookCopyId
transactionId
```

### OTP Invalid Or Missing

The OTP expires after 10 minutes. Run signup again with a new email, then copy the new OTP from your configured notification path.

### Kafka Or Redis Errors

Make sure Docker services are running:

```powershell
docker compose up -d postgres redis kafka
```
