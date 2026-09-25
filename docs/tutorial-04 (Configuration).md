# Tutorial 04 — Configuration

Keep configuration outside the code and make the app behave differently in dev and prod.

Files for this stage:
- `src/main/resources/application.properties`
- `src/main/resources/application-dev.properties`
- `src/main/resources/application-prod.properties`
- `src/main/java/com/example/bookshop/config/BookshopProperties.java`
- `src/main/java/com/example/bookshop/controller/ShopController.java`
- `src/main/java/com/example/bookshop/BookshopApplication.java`

---

## 1. Put config in properties files

Instead of hardcoding values, keep them in `application.properties`:

```properties
bookshop.shop-name=Bookshop
bookshop.currency=USD
bookshop.catalog.max-page-size=100
```

This lets the same app run with different values in different environments.

## 2. Read values with @Value

Simple case:

```java
@Value("${bookshop.currency}")
private String currency;
```

This pulls the value from the config at startup.

For many settings, it is better to bind a whole group together.

## 3. Read a whole config block with @ConfigurationProperties

```java
@ConfigurationProperties(prefix = "bookshop")
public class BookshopProperties {

    private String shopName;
    private String currency;

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
```

Then enable it in the app:

```java
@SpringBootApplication
@ConfigurationPropertiesScan
public class BookshopApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookshopApplication.class, args);
    }
}
```

This is cleaner than reading each value separately.

## 4. Fail fast on bad config

Add validation:

```java
@ConfigurationProperties(prefix = "bookshop")
@Validated
public class BookshopProperties {

    @NotBlank
    private String shopName;

    @NotBlank
    private String currency;
}
```

If the value is missing or invalid, the app fails at startup instead of failing later during requests.

## 5. Profiles: dev vs prod

Use different property files:

```text
application.properties        # base config
application-dev.properties     # dev overrides
application-prod.properties    # prod overrides
```

Activate a profile:

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

This lets you change behavior without changing code.

## 6. Environment variables override files

You can override config with environment variables:

```bash
BOOKSHOP_CURRENCY=EUR ./mvnw spring-boot:run
```

Spring automatically maps this to the property `bookshop.currency`.

Order of precedence:

1. `application.properties`
2. profile-specific file
3. environment variables
4. command-line arguments

## 7. Secrets must not live in source files

Never put real secrets in `application.properties`.
Use environment variables or secret managers in production.

Examples:
- DB passwords
- JWT secret
- API keys

## 8. Goal for this tutorial

By the end of this tutorial, you should understand:
- why config lives outside code
- how `@ConfigurationProperties` works
- how profiles switch behavior
- how environment variables override defaults

Next: [**Tutorial 05 — Database and JPA**](tutorial-05%20%28Database%20and%20JPA%29.md)
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

Next: [**Tutorial 05 — Database and JPA**](tutorial-05%20%28Database%20and%20JPA%29.md): H2, @Entity, JpaRepository,
and watching the SQL Spring generates.
