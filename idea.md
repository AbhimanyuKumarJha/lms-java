Start fresh with a small **in-memory Spring Boot design**. Since you are not using a database, these are plain Java model/domain classes, not necessarily JPA `@Entity` classes.

The simplest useful design is:

```text
Book
BookCopy
User
Issue
BookCopyStatus
IssueStatus
LibraryService
```

## 1. `Book`

`Book` represents the general details of a title.

For example, **Clean Code** is one `Book`, even when the library owns five physical copies.

```java
public class Book {

    private Long bookId;
    private String title;
    private String author;
    private String description;

    public Book(Long bookId, String title, String author, String description) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.description = description;
    }

    // Getters and setters
}
```

Do not put `totalBookCount()` inside `Book`.

A single `Book` object does not know how many copies exist in the entire library. That operation belongs in `LibraryService`.

For example:

```java
public long getTotalCopies(Long bookId)
```

## 2. `BookCopy`

A `BookCopy` represents one physical copy of a book.

Avoid using Java inheritance here:

```java
class BookCopy extends Book
```

A physical copy **is not a different type of book**. It is a copy that refers to a book.

Use composition:

```java
public class BookCopy {

    private Long bookCopyId;
    private Book book;
    private String copyCode;
    private BookCopyStatus status;

    public BookCopy(Long bookCopyId, Book book, String copyCode) {
        this.bookCopyId = bookCopyId;
        this.book = book;
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

    // Getters and setters
}
```

Instead of storing `bookDetails` separately, store:

```java
private Book book;
```

You can then access:

```java
bookCopy.getBook().getTitle();
bookCopy.getBook().getAuthor();
```

## 3. `BookCopyStatus`

The status of the physical copy belongs to `BookCopy`.

```java
public enum BookCopyStatus {
    AVAILABLE,
    ISSUED,
    LOST,
    DAMAGED
}
```

For your first version, these four values are enough.

Do not add `OVERDUE` here. A physical copy is issued; the borrowing transaction becomes overdue.

## 4. `User`

Keep the `User` model simple.

```java
import java.time.LocalDateTime;

public class User {

    private Long userId;
    private String name;
    private LocalDateTime createdAt;

    public User(Long userId, String name) {
        this.userId = userId;
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
}
```

These methods should generally not be inside `User`:

```text
getIssuedBooks()
getFineDetails()
totalFine()
```

The reason is that the `User` object does not itself store every transaction in the library. The service holds and searches the issue records.

Put them in `LibraryService`:

```java
List<Issue> getIssuedBooks(Long userId);

BigDecimal getTotalFine(Long userId);
```

A model class should mainly represent data and small behaviour related directly to itself.

## 5. `Issue`

Your `Issues` class should preferably be singular:

```java
Issue
```

You can also name it:

```java
BorrowTransaction
```

`Issue` is acceptable for learning.

```java
import java.math.BigDecimal;
import java.time.LocalDate;

public class Issue {

    private Long transactionId;
    private User user;
    private BookCopy bookCopy;
    private LocalDate issuedAt;
    private LocalDate dueDate;
    private LocalDate returnedAt;
    private IssueStatus status;

    public Issue(
            Long transactionId,
            User user,
            BookCopy bookCopy,
            LocalDate issuedAt,
            LocalDate dueDate
    ) {
        this.transactionId = transactionId;
        this.user = user;
        this.bookCopy = bookCopy;
        this.issuedAt = issuedAt;
        this.dueDate = dueDate;
        this.status = IssueStatus.ACTIVE;
    }

    public boolean isOverdue() {
        return status == IssueStatus.ACTIVE
                && LocalDate.now().isAfter(dueDate);
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
        LocalDate effectiveReturnDate =
                returnedAt != null ? returnedAt : LocalDate.now();

        if (!effectiveReturnDate.isAfter(dueDate)) {
            return BigDecimal.ZERO;
        }

        long overdueDays =
                java.time.temporal.ChronoUnit.DAYS.between(
                        dueDate,
                        effectiveReturnDate
                );

        return finePerDay.multiply(BigDecimal.valueOf(overdueDays));
    }

    // Getters and setters
}
```

Store the complete object references:

```java
private User user;
private BookCopy bookCopy;
```

Instead of only:

```java
private Long bookCopyId;
```

This is cleaner for an object-oriented in-memory application.

## 6. `IssueStatus`

The transaction status belongs to `Issue`.

```java
public enum IssueStatus {
    ACTIVE,
    RETURNED,
    LOST,
    CANCELLED
}
```

You do not need to store `OVERDUE` as a status initially.

Overdue can be calculated:

```java
status == ACTIVE && currentDate > dueDate
```

Otherwise, you would need to continuously update the status when the date changes.

## Why status exists in two classes

Having status in both `BookCopy` and `Issue` is correct because they represent different things.

### `BookCopy.status`

Answers:

> What is the current condition or availability of this physical copy?

```text
AVAILABLE
ISSUED
LOST
DAMAGED
```

### `Issue.status`

Answers:

> What is the current state of this borrowing transaction?

```text
ACTIVE
RETURNED
LOST
CANCELLED
```

Example:

```text
BookCopy status = ISSUED
Issue status    = ACTIVE
```

After returning:

```text
BookCopy status = AVAILABLE
Issue status    = RETURNED
```

## Where should fine go?

For your simple version, do not create a separate `Fine` class.

Calculate the fine from the `Issue`:

```text
overdue days × fine per day
```

For example:

```java
issue.calculateFine(new BigDecimal("5.00"));
```

A separate `Fine` class becomes useful only when you want:

- Fine payment history
- Partial payment
- Fine waiver
- Paid and unpaid status
- Multiple fines for damage, loss and overdue

You do not need that complexity now.

## 7. `LibraryService`

The main operations should be handled by a service class.

```java
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class LibraryService {

    private final Map<Long, Book> books = new HashMap<>();
    private final Map<Long, BookCopy> bookCopies = new HashMap<>();
    private final Map<Long, User> users = new HashMap<>();
    private final List<Issue> issues = new ArrayList<>();

    public void addBook(Book book) {
        books.put(book.getBookId(), book);
    }

    public void addBookCopy(BookCopy bookCopy) {
        bookCopies.put(bookCopy.getBookCopyId(), bookCopy);
    }

    public void addUser(User user) {
        users.put(user.getUserId(), user);
    }

    public Issue issueBook(
            Long transactionId,
            Long userId,
            Long bookCopyId
    ) {
        User user = users.get(userId);
        BookCopy bookCopy = bookCopies.get(bookCopyId);

        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        if (bookCopy == null) {
            throw new IllegalArgumentException("Book copy not found");
        }

        if (!bookCopy.isAvailable()) {
            throw new IllegalStateException("Book copy is not available");
        }

        Issue issue = new Issue(
                transactionId,
                user,
                bookCopy,
                LocalDate.now(),
                LocalDate.now().plusDays(14)
        );

        bookCopy.markIssued();
        issues.add(issue);

        return issue;
    }

    public void returnBook(Long transactionId) {
        Issue issue = findIssue(transactionId);
        issue.returnBook();
    }

    public List<Issue> getIssuedBooks(Long userId) {
        return issues.stream()
                .filter(issue ->
                        issue.getUser().getUserId().equals(userId))
                .filter(issue ->
                        issue.getStatus() == IssueStatus.ACTIVE)
                .toList();
    }

    public BigDecimal getTotalFine(Long userId) {
        BigDecimal finePerDay = new BigDecimal("5.00");

        return issues.stream()
                .filter(issue ->
                        issue.getUser().getUserId().equals(userId))
                .map(issue ->
                        issue.calculateFine(finePerDay))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long getTotalCopies(Long bookId) {
        return bookCopies.values()
                .stream()
                .filter(copy ->
                        copy.getBook().getBookId().equals(bookId))
                .count();
    }

    public long getAvailableCopies(Long bookId) {
        return bookCopies.values()
                .stream()
                .filter(copy ->
                        copy.getBook().getBookId().equals(bookId))
                .filter(BookCopy::isAvailable)
                .count();
    }

    private Issue findIssue(Long transactionId) {
        return issues.stream()
                .filter(issue ->
                        issue.getTransactionId().equals(transactionId))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Issue transaction not found"
                        ));
    }
}
```

## Final relationship design

```text
Book 1 -------- many BookCopy

User 1 -------- many Issue

BookCopy 1 ---- many Issue
                over its lifetime

Issue --------- one User
Issue --------- one BookCopy
```

A book copy can have many historical issues, but only one active issue at a time.

## Suggested project structure

```text
src/main/java/com/example/library
│
├── model
│   ├── Book.java
│   ├── BookCopy.java
│   ├── User.java
│   ├── Issue.java
│   ├── BookCopyStatus.java
│   └── IssueStatus.java
│
├── service
│   └── LibraryService.java
│
├── controller
│   └── LibraryController.java
│
└── LibraryApplication.java
```

## Final simplified model

| Class            | Main responsibility                       |
| ---------------- | ----------------------------------------- |
| `Book`           | Stores common book information            |
| `BookCopy`       | Represents one physical copy              |
| `User`           | Stores member information                 |
| `Issue`          | Records issuing and returning             |
| `LibraryService` | Performs searches and business operations |

Your original idea is directionally correct. The main corrections are:

1. `BookCopy` should reference `Book`, not extend it.
2. Count and lookup operations should go into `LibraryService`.
3. Fine can be calculated from `Issue`.
4. Two different statuses are valid because copy status and transaction status mean different things.
5. Use singular class names such as `Issue`, not `Issues`.
