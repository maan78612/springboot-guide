# Tutorial 14 — Logging

SLF4J, levels, configuring output, dev-pretty vs prod-JSON, and what
must never be logged.

Files for this stage:
- `service/BookService.java` (a logger + a few meaningful lines)
- `application-dev.properties` (package level DEBUG, file output)
- `application-prod.properties` (structured JSON output)
- `.gitignore` (+ logs/)

---

## 1. Why not System.out.println

You have used `println` all through your Java course. In a server it
fails four ways: no timestamps, no severity, no way to turn lines
off without editing code, and no machine-readable format for the
tools that read production logs. A logging framework fixes all four.

The two names:

```
+---------+---------------------------------------------------------+
| SLF4J   | the FACADE - the interface our code writes against      |
| Logback | the ENGINE behind it - formats, writes, rotates         |
+---------+---------------------------------------------------------+
```

Code depends only on SLF4J; Spring Boot wires Logback behind it.
(Same pattern as JPA/Hibernate in tutorial 05.)

## 2. The pattern, once per class

```java
private static final Logger log = LoggerFactory.getLogger(BookService.class);
```

One logger per class, named after the class — the name is what
levels are filtered by. Then:

```java
log.info("Book created: id={}, authorId={}", saved.getId(), request.authorId());
```

**Always `{}` placeholders, never string `+`.** Two reasons: the
message is only assembled if the level is actually on (a `+` chain
builds the string even when DEBUG is off — wasted work on every
call), and values stay visually separate from text.

## 3. Levels

```
+-------+---------------------------------------------+------------+
| Level | Means                                       | Prod?      |
+-------+---------------------------------------------+------------+
| ERROR | something broke; a human should look        | always on  |
| WARN  | suspicious but survivable (retry worked,    | always on  |
|       | deprecated call, config fallback)           |            |
| INFO  | notable business events: created, deleted,  | on         |
|       | restored, discount applied, app started     |            |
| DEBUG | developer detail: search parameters,        | off        |
|       | decisions taken, sizes                      |            |
| TRACE | firehose (every step) - rarely worth it     | off        |
+-------+---------------------------------------------+------------+
```

Where we log now: the exception handler ERRORs every unexpected
exception (tutorial 09 — the most important log line in the app);
the service INFOs the four business events and DEBUGs search
parameters. Note what we do NOT log: no "entering method X" —
narration is noise.

## 4. The common confusion — "my DEBUG line doesn't print"

Verified: with the debug line in place and no configuration, the
search endpoint logged NOTHING. The default level is INFO — DEBUG
and TRACE are dropped. This is the number-one beginner logging
question, and it is configuration, not code:

```properties
# application-dev.properties
logging.level.com.example.bookshop=DEBUG    # our package only
```

After that (verified):

```
DEBUG ... c.example.bookshop.service.BookService : Book search:
search=java, authorId=null, genreId=null, page=1, size=10
```

Levels are per logger-name prefix, so the framework stays at INFO
while OUR code gets DEBUG. The same mechanism controls third-party
noise: `logging.level.org.hibernate.SQL=DEBUG` is what
`spring.jpa.show-sql` does, with more control.

## 5. Where logs go

**Console** — always, and in dev that is all you need.

**A file** — one property (verified; rotation at 10 MB is automatic):

```properties
logging.file.name=logs/bookshop.log
```

`logs/` is in `.gitignore` — logs are runtime output, never source.

**Production: one JSON object per line.** Log collectors (Elastic,
Datadog, CloudWatch) don't want pretty columns, they want fields to
index. Boot has it built in — our prod profile sets:

```properties
logging.structured.format.console=ecs
```

Real output from a prod-profile run:

```json
{"@timestamp":"2026-09-03T10:03:04.251057Z","log":{"level":"INFO",
 "logger":"com.example.bookshop.BookshopApplication"},"process":{"pid":17371,...},
 "service":{"name":"bookshop",...},"message":"Started BookshopApplication in 1.629 seconds..."}
```

Same log calls, different rendering per profile: humans read dev,
machines read prod.

## 6. What never goes into a log

A log is plain text, copied to more places than the database and
kept for months. Rules:

- **Never secrets**: passwords (even wrong ones from a failed
  login!), tokens, API keys, session ids.
- **Never personal data you don't need**: log ids, not people.
  "Order 812 failed for user 42", not the user's name and address.
- **Be careful echoing user input**: our DEBUG line logs the raw
  `search` string — acceptable at DEBUG in dev, but know that input
  containing newlines can forge fake log lines in naive setups;
  collectors that treat one JSON object as one event (our prod
  format) are immune.
- ERROR should carry the exception object (`log.error("...", ex)`) —
  the stack trace is the payload. Log it ONCE, where it is handled
  (our advice class), not at every layer it passes through.

## 7. Recap

- SLF4J facade + Logback engine; one static logger per class;
  `{}` placeholders always.
- Levels: business events at INFO, developer detail at DEBUG —
  which is OFF by default; `logging.level.<package>` turns it on
  per prefix.
- Files and JSON output are configuration, not code — dev pretty,
  prod ECS JSON, verified both.
- No secrets, no unnecessary personal data, exceptions logged once
  with their stack.

Next: [**Tutorial 15 — Auth**](tutorial-15%20%28Auth%29.md): Spring Security's locked-down
defaults, password hashing with BCrypt, register/login endpoints,
JWT tokens, and seeding the first admin.
