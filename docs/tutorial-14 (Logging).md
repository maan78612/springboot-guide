# Tutorial 14 — Logging

Use a real logger instead of raw `System.out.println`.

Files for this stage:

- Updated: `src/main/java/com/example/bookshop/service/BookService.java`
- Updated: `src/main/resources/application-dev.properties`
- Updated: `src/main/resources/application-prod.properties`

---

## 1. Add a logger

```java
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

## 2. Use log levels correctly

```properties
logging.level.com.example.bookshop=DEBUG
```

This turns on debug logs for your package without turning on noisy framework output.

## 3. Avoid leaking secrets

Do not log passwords, tokens, or personal data unless absolutely needed.

Next: [**Tutorial 15 — Auth**](tutorial-15%20%28Auth%29.md)
