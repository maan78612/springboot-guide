# Tutorial 04 — Configuration

application.properties, @Value, @ConfigurationProperties with
fail-fast validation, profiles (dev vs prod), environment variables,
and where secrets must NOT go.

Files for this stage:
- `src/main/resources/application.properties` (grew)
- `src/main/resources/application-dev.properties` (new)
- `src/main/resources/application-prod.properties` (new)
- `config/BookshopProperties.java` (new)
- `controller/ShopController.java` (new)
- `BookshopApplication.java` (+ @ConfigurationPropertiesScan)
- `pom.xml` (+ spring-boot-starter-validation)

---

## 1. Why configuration lives outside the code

Some values must change WITHOUT recompiling: the port, the database
address, page-size limits, feature switches. The same jar should run
on your laptop and in production — only its settings differ. (Same
reason your Node repo reads `process.env` instead of hardcoding.)

Spring's home for those values is
`src/main/resources/application.properties` — plain `key=value`
lines. Two kinds of keys live there:

```
+---------------------------+---------------------------------------+
| Spring's own keys         | Your own keys                         |
+---------------------------+---------------------------------------+
| server.port,              | Any prefix you invent. Ours is        |
| spring.application.name,  | bookshop.* :                          |
| thousands more - they     |   bookshop.shop-name=Bookshop         |
| configure the framework   |   bookshop.currency=USD               |
|                           |   bookshop.catalog.max-page-size=100  |
+---------------------------+---------------------------------------+
```

## 2. Reading one value: @Value

The first version of `ShopController` read settings like this:

```java
@Value("${bookshop.currency}")
private String currency;
```

"At startup, look up `bookshop.currency` and inject it here." It
works — `GET /api/v1/shop` returned:

```
{"currency":"USD","name":"Bookshop"}
```

**The common mistake for this topic.** I misspelled the key as
`${bookshop.currencyy}`. The app refuses to start:

```
Caused by: org.springframework.util.PlaceholderResolutionException:
Could not resolve placeholder 'bookshop.currencyy' in value "${bookshop.currencyy}"
```

The message names the bad key, but not WHERE you wrote it — with
twenty `@Value`s scattered across the app, you grep. Which leads to
the better tool.

## 3. Reading a group of values: @ConfigurationProperties

`BookshopProperties` is the whole `bookshop.*` section as one typed
bean:

```java
@ConfigurationProperties(prefix = "bookshop")
@Validated
public class BookshopProperties {
    @NotBlank private String shopName;
    @NotBlank private String currency;
    @Valid    private final Catalog catalog = new Catalog();
    // getters and setters ...
}
```

At startup Spring "binds" the file to the object: it matches
`bookshop.shop-name` to `setShopName(...)` (kebab-case maps to
camelCase automatically) and `bookshop.catalog.max-page-size` to the
nested `Catalog` object. One extra line makes Spring find the class —
`@ConfigurationPropertiesScan` on `BookshopApplication`.

Consumers now inject it like any bean (see the rewritten
`ShopController`) — configuration is just another dependency.

```
+----------------------------+---------------------------------------+
| @Value                     | @ConfigurationProperties              |
+----------------------------+---------------------------------------+
| one key per annotation     | a whole prefix as one typed object    |
| key string repeated at     | key names exist in exactly one class  |
| every reader               |                                       |
| typo found when the        | typo = unknown key is simply ignored, |
| READER class is created    | but a missing/blank value fails the   |
|                            | bind (with validation below)          |
| fine for a single value    | the default for real projects         |
+----------------------------+---------------------------------------+
```

## 4. Fail fast: validate config at startup

`@Validated` plus constraints (`@NotBlank`, `@Min(1)`, `@Max(500)`)
make bad config kill the boot instead of surfacing at 2 a.m. on the
first request that uses the value. This is the Spring version of the
`env.js` validation in your Node repo.

Verified: I set `bookshop.catalog.max-page-size=0` and started:

```
***************************
APPLICATION FAILED TO START
***************************

Description:

Binding to target com.example.bookshop.config.BookshopProperties failed:

    Property: bookshop.catalog.maxPageSize
    Value: "0"
    Reason: must be greater than or equal to 1

Action:

Update your application's configuration
```

Exact property, exact value, exact reason. This is the best error
message in Spring Boot; make your config fail like this on purpose.
(The constraint annotations get their full tutorial at stage 08.)

## 5. Profiles: dev vs prod

> A **profile** is a named set of configuration. Activate a profile
> and its file is loaded ON TOP of application.properties — same
> keys win, everything else falls through.

The file naming convention does all the work:

```
application.properties        always loaded (the base)
application-dev.properties    loaded when profile "dev" is active
application-prod.properties   loaded when profile "prod" is active
```

Our `application-dev.properties` overrides one key
(`bookshop.shop-name=Bookshop (dev)`) so the effect is visible.
Real differences (SQL logging, database address, log format) arrive
in tutorials 14, 18 and 19.

Verified, three runs:

```bash
./mvnw spring-boot:run                              # no profile
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run   # dev profile
```

```
=== no profile ===
log:  No active profile set, falling back to 1 default profile: "default"
curl: {"name":"Bookshop","currency":"USD"}

=== dev profile ===
log:  The following 1 profile is active: "dev"
curl: {"currency":"USD","name":"Bookshop (dev)"}
```

! Warning, the silent version of this topic's mistake: name the file
wrong — `application_dev.properties` or `application-Dev.properties` —
and NOTHING complains. The file is simply never loaded and you get
base values. If a profile "isn't working", check the filename and the
startup log's "profile is active" line first.

## 6. Environment variables beat the file

Every property can be overridden from outside, without touching any
file. Spring translates env-var names automatically ("relaxed
binding"): dots become underscores, everything uppercase.

```
+--------------------------------+--------------------------+
| Property key                   | Environment variable     |
+--------------------------------+--------------------------+
| bookshop.currency              | BOOKSHOP_CURRENCY        |
| server.port                    | SERVER_PORT              |
| spring.profiles.active         | SPRING_PROFILES_ACTIVE   |
+--------------------------------+--------------------------+
```

Verified:

```
BOOKSHOP_CURRENCY=EUR ./mvnw spring-boot:run
curl: {"name":"Bookshop","currency":"EUR"}      <- file says USD

SERVER_PORT=8081 ./mvnw spring-boot:run
log:  Tomcat started on port 8081 (http)        <- tutorial 01's
                                                   port-conflict fix
```

Precedence, simplified — later wins:

1. `application.properties` (base)
2. `application-<profile>.properties` (active profile)
3. environment variables
4. command-line args (`--server.port=8081`)

This is exactly how production works (tutorial 19): the SAME jar,
configured entirely by environment variables.

## 7. Where secrets do NOT go

> A **secret** is any value that grants access: passwords, API keys,
> tokens, connection strings with passwords in them.

Rules, non-negotiable:

- Never in `application.properties` or any file that goes into git.
  Git history is forever — deleting the line later does not unpublish
  it.
- Never in code, never in logs.
- Secrets enter through environment variables (or a secret manager in
  bigger setups): the file holds the KEY with no value, or a harmless
  default: `bookshop.admin-password=${ADMIN_PASSWORD:}`.
- The repo documents WHICH variables are required (your Node repo's
  `.env.example` — ours lands in the README at the template stage),
  never their values.

We have no secrets yet. The first real one arrives with the JWT
signing key in tutorial 15, and it will follow these rules.

## 8. Recap

- `application.properties` = key=value config; your keys get a prefix
  (`bookshop.*`).
- `@Value` for a one-off; `@ConfigurationProperties` class for real
  projects — typed, greppable, validated.
- `@Validated` + constraints = the app refuses to boot on bad config.
- Profiles stack a named file on top of the base; env vars beat both;
  a misnamed profile file fails silently.
- Secrets never enter git. Env vars are the front door.

Next: **Tutorial 05 — Database and JPA**: H2, @Entity, JpaRepository,
and watching the SQL Spring generates.
