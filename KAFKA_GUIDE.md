# Kafka Guide for LMS

This project uses Kafka as the event bus between LMS modules. The main business flow still happens in the database first. Kafka is used after that to announce that something important happened, such as a signup OTP being requested, a user being verified, a book being borrowed, a book being returned, or a fine being generated.

In simple terms:

1. A REST API receives a request.
2. A service changes the database or Redis.
3. The service asks `DomainEventPublisher` to publish an event.
4. `DomainEventPublisher` waits until the database transaction commits.
5. Kafka receives the event on a topic.
6. Listener classes consume the event in separate consumer groups.

## Important Files

| File | Purpose |
| --- | --- |
| `src/main/java/com/example/lms/shared/events/EventNames.java` | Central list of Kafka topic names. |
| `src/main/java/com/example/lms/shared/events/DomainEventPublisher.java` | Shared publisher used by services to send Kafka events after commit. |
| `src/main/java/com/example/lms/shared/config/KafkaClientConfig.java` | Producer, consumer, listener, and admin configuration. |
| `src/main/java/com/example/lms/shared/config/KafkaTopicConfig.java` | Creates the project Kafka topics. |
| `src/main/resources/application.yaml` | Kafka bootstrap server and serializer/deserializer settings. |
| `src/main/java/com/example/lms/notifications/NotificationListener.java` | Notification consumer group. Currently subscribed, but method bodies are empty. |
| `src/main/java/com/example/lms/reports/ReportProjectionListener.java` | Report projection consumer group. Currently subscribed, but method bodies are empty. |

## Kafka Topics

All topic names live in `EventNames`.

| Topic | Event Class | Published When | Key |
| --- | --- | --- | --- |
| `signup-otp-requested` | `SignupOtpRequestedEvent` | A user signs up through `/api/auth/signup` and an OTP is created. | User email |
| `user-verified` | `UserVerifiedEvent` | A signup OTP is successfully verified through `/api/otp/verify`. | User email |
| `user-created` | `UserCreatedEvent` | A user is created directly through `/api/users`. | User ID |
| `book-borrowed` | `BookBorrowedEvent` | A book copy is issued through `/api/issues`. | Transaction ID |
| `book-returned` | `BookReturnedEvent` | A borrowed book is returned through `/api/issues/{transactionId}/return`. | Transaction ID |
| `fine-generated` | `FineGeneratedEvent` | A returned book is overdue and a fine is calculated. | Transaction ID |

## Topic Creation

`KafkaTopicConfig` defines one `NewTopic` bean per topic.

Each topic is created with:

- `partitions(1)`
- `replicas(1)`

That is a local-development setup. With one partition, messages inside a topic are processed in the same order they are written. With one replica, Kafka does not duplicate the topic across brokers, which is fine for local Docker but not production-safe.

Kafka admin creation is configured in `KafkaClientConfig`.

```java
kafkaAdmin.setAutoCreate(autoCreate);
kafkaAdmin.setFatalIfBrokerNotAvailable(false);
```

So the app tries to create topics automatically, and it does not fail application startup immediately if Kafka is not available.

## Producer Configuration

`KafkaClientConfig` creates:

- `ProducerFactory<String, Object>`
- `KafkaTemplate<String, Object>`

Keys are strings:

```java
StringSerializer
```

Values are JSON:

```java
JsonSerializer
```

The producer also adds Java type information headers:

```java
JsonSerializer.ADD_TYPE_INFO_HEADERS = true
```

That matters because consumers receive `Object` values. The type header lets Spring Kafka know whether the JSON should become `BookBorrowedEvent`, `UserCreatedEvent`, `FineGeneratedEvent`, and so on.

## Consumer Configuration

`KafkaClientConfig` creates:

- `ConsumerFactory<String, Object>`
- `ConcurrentKafkaListenerContainerFactory<String, Object>`

Keys use:

```java
StringDeserializer
```

Values use:

```java
JsonDeserializer
```

The consumer trusts only these event packages:

```text
com.example.lms.borrowing.events
com.example.lms.fines.events
com.example.lms.auth.events
com.example.lms.users.events
```

That is important security-wise. JSON deserialization should not blindly instantiate classes from any package.

The default consumer group from `application.yaml` is:

```yaml
spring.kafka.consumer.group-id: lms-modular-monolith
```

But the listener methods override this with their own group IDs.

## Consumer Groups

Kafka consumer groups decide who gets a copy of each message.

This app has two named listener groups:

| Group ID | Class | Purpose |
| --- | --- | --- |
| `lms-notifications` | `NotificationListener` | Intended for sending OTP, borrow, return, and fine notifications. |
| `lms-reports` | `ReportProjectionListener` | Intended for updating reporting/read-model projections. |

Different groups each receive their own copy of the same event.

Example: when `book-borrowed` is published:

```text
book-borrowed topic
    -> lms-notifications receives it
    -> lms-reports receives it
```

If there were two instances of `lms-notifications` in the same group, Kafka would split partitions between them. Since these topics currently have one partition, only one consumer instance in that group would process messages for a topic at a time.

## Why `publishAfterCommit` Matters

All business services publish through `DomainEventPublisher`.

```java
eventPublisher.publishAfterCommit(topic, key, event);
```

Internally it checks whether a Spring transaction is active.

If there is no transaction, it publishes immediately:

```java
publish(topic, key, event);
```

If there is an active transaction, it registers a transaction synchronization:

```java
afterCommit() {
    publish(topic, key, event);
}
```

This is the most important design detail in the project.

It means Kafka is only told about successful database changes. For example, if issuing a book fails and the transaction rolls back, the app should not publish `book-borrowed`.

The current implementation sends to Kafka after commit but does not wait for Kafka acknowledgement or retry failed sends. So the database can commit successfully and the Kafka send can still fail afterward. For local learning this is fine, but production systems usually add an outbox table or retry mechanism for stronger delivery guarantees.

## Flow 1: Signup User and OTP Requested

Endpoint:

```http
POST /api/auth/signup
```

Service:

```text
AuthService.signup()
```

What happens:

1. The email is normalized to lowercase.
2. The app checks whether the email already exists.
3. A random 10-digit user ID is generated.
4. A `LibraryUser` is saved with:
   - name
   - email
   - encoded password
   - `emailVerified = false`
5. `OtpService.createSignupOtp(email)` generates a 6-digit OTP.
6. The OTP is hashed and stored in Redis under:

```text
otp:signup:{email}
```

7. The Redis value expires after `lms.otp.ttl-minutes`, which is currently `10`.
8. `AuthService` publishes `SignupOtpRequestedEvent` after the transaction commits.

Topic:

```text
signup-otp-requested
```

Kafka key:

```text
email
```

Payload:

```java
public record SignupOtpRequestedEvent(
        Long userId,
        String email,
        String otp,
        Instant occurredAt
) {}
```

Consumers:

```text
NotificationListener.onSignupOtpRequested()
```

Current behavior:

The listener method exists but is empty. So the event is consumed by the notification group, but no email/SMS is actually sent yet. The OTP is returned in the event payload and is currently suitable for local/demo notification wiring.

Flow:

```text
Client
  -> POST /api/auth/signup
  -> AuthService
  -> PostgreSQL saves unverified user
  -> Redis stores hashed OTP
  -> transaction commits
  -> Kafka topic signup-otp-requested
  -> lms-notifications consumer
```

## Flow 2: User Verified

Endpoint:

```http
POST /api/otp/verify
```

Service:

```text
OtpService.verifySignupOtp()
```

What happens:

1. The email is normalized to lowercase.
2. The user is loaded from PostgreSQL.
3. If the user is already verified, the request fails.
4. Redis is checked for:

```text
otp:signup:{email}
```

5. If no Redis value exists, the OTP is expired or missing.
6. The submitted OTP is hashed using the same email and secret.
7. The submitted hash is compared with the stored hash.
8. If it matches, `user.markEmailVerified()` sets `emailVerified = true`.
9. The Redis OTP key is deleted.
10. `UserVerifiedEvent` is published after the transaction commits.

Topic:

```text
user-verified
```

Kafka key:

```text
email
```

Payload:

```java
public record UserVerifiedEvent(
        Long userId,
        String email,
        Instant occurredAt
) {}
```

Consumers:

There is currently no listener method for `user-verified`.

That means the event is published to Kafka, but no app component currently reacts to it. It is ready for future features such as welcome notifications, audit logs, or reporting projections.

Flow:

```text
Client
  -> POST /api/otp/verify
  -> OtpService
  -> Redis validates OTP hash
  -> PostgreSQL marks user verified
  -> Redis deletes OTP key
  -> transaction commits
  -> Kafka topic user-verified
```

## Flow 3: Direct User Created

Endpoint:

```http
POST /api/users
```

Service:

```text
UserService.createUser()
```

This is separate from signup.

Signup creates an unverified user and publishes `signup-otp-requested`.
Direct user creation saves a user and publishes `user-created`.

What happens:

1. The app checks whether the email already exists.
2. A `LibraryUser` is saved using the provided user ID.
3. The password is encoded.
4. User-related caches are evicted.
5. `UserCreatedEvent` is published after the transaction commits.

Topic:

```text
user-created
```

Kafka key:

```text
userId
```

Payload:

```java
public record UserCreatedEvent(
        Long userId,
        String email,
        String name,
        Instant occurredAt
) {}
```

Consumers:

```text
ReportProjectionListener.onUserCreated()
```

Current behavior:

The reports listener method exists but is empty. It is the correct place to update reporting tables or a read model later.

Flow:

```text
Client
  -> POST /api/users
  -> UserService
  -> PostgreSQL saves user
  -> transaction commits
  -> Kafka topic user-created
  -> lms-reports consumer
```

## Flow 4: Book Issued / Borrowed

Endpoint:

```http
POST /api/issues
```

Service:

```text
IssueService.createIssue()
```

What happens:

1. `IssueService.createIssue()` starts a transaction through `TransactionTemplate`.
2. The user is loaded from PostgreSQL.
3. The book copy is loaded with a pessimistic write lock:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<BookCopy> findByIdForUpdate(Long bookCopyId);
```

4. The lock prevents two requests from issuing the same copy at the same time.
5. The app checks `bookCopy.isAvailable()`.
6. An `Issue` is created with:
   - transaction ID
   - user
   - book copy
   - issued date as today
   - due date as today plus 14 days
   - status `ACTIVE`
7. The book copy status changes from `AVAILABLE` to `ISSUED`.
8. The issue transaction is saved.
9. Book/user caches are evicted.
10. `BookBorrowedEvent` is published after the transaction commits.

Topic:

```text
book-borrowed
```

Kafka key:

```text
transactionId
```

Payload:

```java
public record BookBorrowedEvent(
        Long transactionId,
        Long userId,
        Long bookCopyId,
        String title,
        LocalDate issuedAt,
        LocalDate dueDate,
        Instant occurredAt
) {}
```

Consumers:

```text
NotificationListener.onBookBorrowed()
ReportProjectionListener.onBookBorrowed()
```

Current behavior:

Both listener methods are empty. Later, notifications could send a borrow confirmation and reports could increment active borrow counts.

Flow:

```text
Client
  -> POST /api/issues
  -> IssueService
  -> PostgreSQL locks book copy row
  -> app verifies copy is AVAILABLE
  -> book copy becomes ISSUED
  -> issue row is saved as ACTIVE
  -> transaction commits
  -> Kafka topic book-borrowed
  -> lms-notifications consumer
  -> lms-reports consumer
```

## Flow 5: Bulk Book Issue

Endpoint:

```http
POST /api/issues/bulk
```

Service:

```text
IssueService.createIssues()
```

What happens:

1. The request contains multiple issue requests.
2. The service creates a fixed thread pool.
3. Each request is processed by calling `createIssue(request)`.
4. Each individual issue has its own transaction.
5. Each successful issue publishes its own `book-borrowed` event after its own transaction commits.

Important detail:

Bulk issue is not one big all-or-nothing transaction. It is many independent issue operations running concurrently. If one request fails, other requests may still have already committed and published events.

The pessimistic book-copy lock still protects the same copy from being issued twice.

## Flow 6: Book Returned

Endpoint:

```http
POST /api/issues/{transactionId}/return
```

Service:

```text
IssueService.returnBook()
```

What happens:

1. The issue transaction is loaded from PostgreSQL.
2. `issue.returnBook()` is called.
3. If the issue is not `ACTIVE`, the request fails.
4. `returnedAt` is set to today.
5. Issue status changes from `ACTIVE` to `RETURNED`.
6. The related book copy status changes from `ISSUED` to `AVAILABLE`.
7. Book/user caches are evicted.
8. `BookReturnedEvent` is published after the transaction commits.
9. The fine is calculated.
10. If the fine is greater than zero, `FineGeneratedEvent` is also published after the transaction commits.

Topic:

```text
book-returned
```

Kafka key:

```text
transactionId
```

Payload:

```java
public record BookReturnedEvent(
        Long transactionId,
        Long userId,
        Long bookCopyId,
        String title,
        LocalDate returnedAt,
        Instant occurredAt
) {}
```

Consumers:

```text
NotificationListener.onBookReturned()
ReportProjectionListener.onBookReturned()
```

Current behavior:

Both listener methods are empty. Later, notification logic could send a return receipt and report logic could reduce active borrow counts.

Flow:

```text
Client
  -> POST /api/issues/{transactionId}/return
  -> IssueService
  -> PostgreSQL updates issue as RETURNED
  -> PostgreSQL updates book copy as AVAILABLE
  -> fine is calculated
  -> transaction commits
  -> Kafka topic book-returned
  -> lms-notifications consumer
  -> lms-reports consumer
```

## Flow 7: Fine Generated

Fine generation happens inside the return flow.

Service:

```text
FineService.calculateFine()
```

Fine rule:

```text
5.00 per overdue day
```

The `Issue` model calculates the fine like this:

1. Use `returnedAt` if present.
2. Otherwise use today.
3. If the effective return date is not after the due date, fine is `0`.
4. If it is after the due date, calculate overdue days.
5. Multiply overdue days by `5.00`.

If the amount is greater than zero, `IssueService` publishes `FineGeneratedEvent`.

Topic:

```text
fine-generated
```

Kafka key:

```text
transactionId
```

Payload:

```java
public record FineGeneratedEvent(
        Long transactionId,
        Long userId,
        BigDecimal amount,
        LocalDate dueDate,
        LocalDate returnedAt,
        Instant occurredAt
) {}
```

Consumers:

```text
NotificationListener.onFineGenerated()
```

Current behavior:

The listener method is empty. Later, it could notify the user about the fine or trigger payment/accounting workflows.

Flow when fine exists:

```text
Book return transaction commits
  -> Kafka topic book-returned
  -> Kafka topic fine-generated
  -> lms-notifications receives fine-generated
```

## Event Payload Design

The project uses Java records for event payloads.

That keeps events immutable and simple. For example:

```java
public record BookBorrowedEvent(
        Long transactionId,
        Long userId,
        Long bookCopyId,
        String title,
        LocalDate issuedAt,
        LocalDate dueDate,
        Instant occurredAt
) {}
```

Each event contains IDs plus enough display/context fields for consumers.

For example, `BookBorrowedEvent` includes `title`. A notification consumer can send "You borrowed X" without immediately querying the book table again.

## Event Keys

Keys matter in Kafka because messages with the same key go to the same partition.

Current keys:

- User lifecycle events use email or user ID.
- Borrow/return/fine events use transaction ID.

Because every topic currently has one partition, key choice does not change ordering much right now. If the topics later get more partitions, the key will decide ordering boundaries.

For example, all events for transaction `9001` would stay ordered within the same partition if they use `9001` as the key.

## What Is Currently Real vs Stubbed

Real and active:

- Kafka producer setup
- Kafka consumer setup
- Topic beans
- JSON event serialization
- JSON event deserialization
- After-commit publishing
- Signup OTP requested event publishing
- User verified event publishing
- Direct user created event publishing
- Book borrowed event publishing
- Book returned event publishing
- Fine generated event publishing

Stubbed for future work:

- Actual notification sending
- Actual report projection updates
- Any consumer for `user-verified`

So when you run the app, Kafka events are produced, and listener methods are registered. But most listener method bodies currently do nothing.

## Local Kafka Setup

`docker-compose.yml` starts Kafka using:

```yaml
image: apache/kafka:3.7.2
ports:
  - "9092:9092"
```

The Spring app connects with:

```yaml
spring.kafka.bootstrap-servers: localhost:9092
```

Start infrastructure:

```bash
docker compose up -d postgres redis kafka
```

Then start the Spring Boot app.

## Useful Kafka Commands

List topics:

```bash
docker exec -it lms-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

Describe a topic:

```bash
docker exec -it lms-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic book-borrowed
```

Read events from the beginning:

```bash
docker exec -it lms-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic book-borrowed --from-beginning
```

Read events and show keys:

```bash
docker exec -it lms-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic book-borrowed --from-beginning --property print.key=true --property key.separator=" -> "
```

Check consumer groups:

```bash
docker exec -it lms-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
```

Describe the notification group:

```bash
docker exec -it lms-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group lms-notifications
```

Describe the reports group:

```bash
docker exec -it lms-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group lms-reports
```

## End-to-End Example

Signup:

```text
POST /api/auth/signup
  -> user saved as unverified
  -> OTP stored in Redis
  -> signup-otp-requested event published
```

Verify:

```text
POST /api/otp/verify
  -> OTP checked against Redis hash
  -> user marked verified
  -> user-verified event published
```

Issue book:

```text
POST /api/issues
  -> book copy locked
  -> issue row saved
  -> copy marked ISSUED
  -> book-borrowed event published
```

Return book:

```text
POST /api/issues/{transactionId}/return
  -> issue marked RETURNED
  -> copy marked AVAILABLE
  -> book-returned event published
  -> fine-generated event published if overdue
```

## Mental Model

Think of the database as the source of truth and Kafka as the announcement system.

The service says:

```text
I changed the state successfully.
Now I will publish an event so other modules can react.
```

The listeners say:

```text
I am interested in this type of event.
When it appears, I can update my own view or send a notification.
```

That design keeps modules loosely coupled. `IssueService` does not need to know how notifications or reports work. It only publishes `book-borrowed` or `book-returned`. Other modules can evolve independently by listening to those topics.
