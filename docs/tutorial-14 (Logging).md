# Tutorial 14 — Logging

Use a real logger instead of raw `System.out.println`. Learn about the SLF4J facade and Logback engine, log levels, parameterized placeholders, dev vs prod JSON log configuration, and how to avoid leaking secrets.

Files for this stage:

- Updated: `src/main/java/com/example/bookshop/service/BookService.java`
- Updated: `src/main/resources/application-dev.properties`
- Updated: `src/main/resources/application-prod.properties`
- Updated: `.gitignore` (add `logs/`)

---

## 1. Why Not `System.out.println`?

In simple scripts, `System.out.println` works, but in production server applications, it fails in four critical ways:
1. **No Timestamps or Context**: It lacks thread names, timestamps, and caller class information.
2. **No Severity Levels**: You cannot distinguish a fatal crash from an informational trace.
3. **Cannot Be Toggled at Runtime**: You cannot mute noisy lines without modifying and recompiling code.
4. **Not Machine-Readable**: Production log aggregators (e.g., Datadog, Elastic, CloudWatch) cannot index raw print statements into structured fields.

### SLF4J vs Logback
Spring Boot uses **SLF4J** as the abstraction API (the facade) and **Logback** as the underlying logging engine:

```text
Application Code
       │
       ▼
┌───────────────────────────┐
│ SLF4J (API / Facade)      │ ──> Provides Logger, LoggerFactory
└───────────────────────────┘
       │
       ▼
┌───────────────────────────┐
│ Logback (Logging Engine)  │ ──> Formats output, rotates files, outputs JSON
└───────────────────────────┘
```

---

## 2. Add a Logger to `BookService`

Add a static logger instance to your class. Always use SLF4J's `org.slf4j.Logger` and `org.slf4j.LoggerFactory`:

```java
package com.example.bookshop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * | Key                  | Why we use it                                     |
 * |----------------------|---------------------------------------------------|
 * | Logger               | Sends structured messages to the logging system  |
 * | static final         | Shares one immutable logger per class             |
 * | LoggerFactory        | Creates a logger named for the class              |
 */
private static final Logger log = LoggerFactory.getLogger(BookService.class);
```

Then log meaningful events like creates, deletes, or filter values using placeholders:

```java
/*
 * | Key                  | Why we use it                                     |
 * |----------------------|---------------------------------------------------|
 * | log.info             | Records a normal operational event                |
 * | {} placeholders      | Inserts values without building a string eagerly  |
 */
log.info("Book created: id={}, title={}", saved.getId(), saved.getTitle());
```

> [!TIP]
> **Always use `{}` placeholders, never string concatenation (`+`)!**
> With `log.debug("Found " + count + " items")`, the JVM allocates strings and concatenates them on **every single call**, even when DEBUG logging is turned off! With `{}` placeholders, SLF4J checks the log level first and only builds the message if the level is actually enabled.

---

## 3. Use Log Levels Correctly

Spring Boot supports five standard log levels:

| Level | Meaning | When to Use | Production Status |
| :--- | :--- | :--- | :--- |
| **ERROR** | Something broke; requires attention. | Unexpected exceptions caught by `GlobalExceptionHandler`. | Always enabled |
| **WARN** | Suspicious or degraded state, but recoverable. | Fallback configurations, deprecations, retry attempts. | Always enabled |
| **INFO** | Normal business milestones. | Application startup, book created, order submitted. | Enabled |
| **DEBUG** | Diagnostic details for developers. | Query parameters, authorization decisions, page sizes. | Disabled by default |
| **TRACE** | Fine-grained internal framework execution. | Method entry/exit, raw network packets. | Disabled |

---

## 4. Configuring Log Levels & Destinations

### Dev Profile: Enable Debug Logs and Local File Output
Open `src/main/resources/application-dev.properties`:

```properties
# Enable DEBUG for our own application package only
logging.level.com.example.bookshop=DEBUG

# Write logs to a local file (automatically rotated at 10MB)
logging.file.name=logs/bookshop.log
```

This turns on debug logs for your package without turning on noisy framework output from Spring or Hibernate.

### Prod Profile: Machine-Readable Structured JSON (ECS)
Open `src/main/resources/application-prod.properties`:

```properties
# Output structured JSON following the Elastic Common Schema (ECS)
logging.structured.format.console=ecs
```

When deployed to production, standard console output emits structured JSON objects that logging agents (Elasticsearch, FluentBit, CloudWatch) index directly:

```json
{
  "@timestamp": "2026-09-03T10:03:04.251Z",
  "log.level": "INFO",
  "message": "Book created: id=42, title=Clean Architecture",
  "service.name": "bookshop"
}
```

---

## 5. Avoid Leaking Secrets (Security Rules)

Logs are plain text files stored across servers, backups, and log aggregators. Follow these critical rules:

1. **Never log sensitive credentials**: Do not log raw passwords, JWTs, API tokens, credit card numbers, or authorization headers.
2. **Minimize PII (Personally Identifiable Information)**: Log resource IDs rather than personal details (e.g., `Order failed for userId=10` rather than printing customer emails and home addresses).
3. **Log Exceptions Once**: Pass the exception instance as the last argument to log the full stack trace (`log.error("Failed to process payment for order {}", orderId, ex);`). Never log the exception at every layer it bubbles through.

---

## Summary Table

| Key / Property | Why we use it |
| :--- | :--- |
| `LoggerFactory.getLogger(Class)` | Instantiates a logger named after the declaring class for prefix filtering. |
| `{}` Placeholders | Defer string construction until after log-level evaluation for high performance. |
| `logging.level.<package>` | Sets minimum log level per package prefix. |
| `logging.file.name` | Configures file output with automated rolling and archiving. |
| `logging.structured.format.console=ecs` | Formats production logs as JSON lines for log aggregators. |

Next: [**Tutorial 15 — Auth**](tutorial-15%20%28Auth%29.md)
