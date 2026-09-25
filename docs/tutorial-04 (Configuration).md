# Tutorial 04 — Configuration

Move config out of code and into Spring properties files.

Files for this stage:

- `src/main/resources/application.properties`
- `src/main/resources/application-dev.properties`
- `src/main/resources/application-prod.properties`
- `src/main/java/com/example/bookshop/config/BookshopProperties.java`
- `src/main/java/com/example/bookshop/controller/ShopController.java`
- `src/main/java/com/example/bookshop/BookshopApplication.java`

---

## 1. Base config: application.properties

```properties
spring.application.name=bookshop

# ── Our own settings (the "bookshop." prefix is ours) ──────────────
bookshop.shop-name=Bookshop
bookshop.currency=USD
bookshop.catalog.default-page-size=10
bookshop.catalog.max-page-size=100

# ── Security (tutorial 15) ─────────────────────────────────────────
bookshop.security.jwt-secret=dev-only-signing-key-0123456789abcdef-not-a-secret
bookshop.security.token-ttl-minutes=60

# ── Database (tutorial 18) ─────────────────────────────────────────
spring.jpa.hibernate.ddl-auto=validate
```

This file holds the base default values for the app.

## 2. Dev overrides: application-dev.properties

```properties
# Loaded ON TOP of application.properties when the "dev" profile is
# active. Keys here win over the same keys there.
bookshop.shop-name=Bookshop (dev)

# See every SQL statement Hibernate runs (dev only - too noisy for prod)
spring.jpa.show-sql=true

# Browser UI for the in-memory database: http://localhost:8080/h2-console
spring.h2.console.enabled=true

# Fixed database name, so the console login URL is always
# jdbc:h2:mem:bookshop (default is a random name per start)
spring.datasource.url=jdbc:h2:mem:bookshop

# ── Logging (tutorial 14) ──────────────────────────────────────────
logging.level.com.example.bookshop=DEBUG
logging.file.name=logs/bookshop.log

# ── Actuator (tutorial 19) ─────────────────────────────────────────
management.endpoint.health.show-details=always

# ── First admin, dev only (tutorial 15) ────────────────────────────
bookshop.admin.email=admin@bookshop.local
bookshop.admin.password=admin-dev-password
```

This file is only used when the `dev` profile is active.

## 3. Prod overrides: application-prod.properties

```properties
# Loaded when the "prod" profile is active.

# ── Logging (tutorial 14) ──────────────────────────────────────────
logging.structured.format.console=ecs

# ── Security (tutorial 15) ─────────────────────────────────────────
bookshop.security.jwt-secret=${JWT_SECRET}
bookshop.admin.email=${ADMIN_EMAIL:}
bookshop.admin.password=${ADMIN_PASSWORD:}

# ── API docs (tutorial 17) ─────────────────────────────────────────
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false

# ── Database (tutorial 18) ─────────────────────────────────────────
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

# ── Production behavior (tutorial 19) ──────────────────────────────
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=20s
```

This file is only used when the `prod` profile is active.

## 4. Enable configuration scanning in the app

```java
package com.example.bookshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BookshopApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookshopApplication.class, args);
    }
}
```

This tells Spring to scan for classes annotated with `@ConfigurationProperties`.

## 5. Typed configuration: BookshopProperties.java

```java
package com.example.bookshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "bookshop")
@Validated
public class BookshopProperties {

    @NotBlank
    private String shopName;

    @NotBlank
    private String currency;

    @Valid
    private final Catalog catalog = new Catalog();

    @Valid
    private final Security security = new Security();

    private final Admin admin = new Admin();

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public Security getSecurity() {
        return security;
    }

    public Admin getAdmin() {
        return admin;
    }

    public static class Security {

        @NotBlank
        @jakarta.validation.constraints.Size(min = 32,
                message = "jwt-secret must be at least 32 characters")
        private String jwtSecret;

        @Min(5)
        private int tokenTtlMinutes = 60;

        private java.util.List<String> corsAllowedOrigins =
                java.util.List.of("http://localhost:3000");

        @Min(1)
        private int authRateLimitPerMinute = 10;

        public String getJwtSecret() {
            return jwtSecret;
        }

        public void setJwtSecret(String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }

        public int getTokenTtlMinutes() {
            return tokenTtlMinutes;
        }

        public void setTokenTtlMinutes(int tokenTtlMinutes) {
            this.tokenTtlMinutes = tokenTtlMinutes;
        }

        public java.util.List<String> getCorsAllowedOrigins() {
            return corsAllowedOrigins;
        }

        public void setCorsAllowedOrigins(java.util.List<String> corsAllowedOrigins) {
            this.corsAllowedOrigins = corsAllowedOrigins;
        }

        public int getAuthRateLimitPerMinute() {
            return authRateLimitPerMinute;
        }

        public void setAuthRateLimitPerMinute(int authRateLimitPerMinute) {
            this.authRateLimitPerMinute = authRateLimitPerMinute;
        }
    }

    public static class Admin {

        private String email = "";
        private String password = "";

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class Catalog {

        @Min(1)
        private int defaultPageSize = 10;

        @Min(1)
        @Max(500)
        private int maxPageSize = 100;

        public int getDefaultPageSize() {
            return defaultPageSize;
        }

        public void setDefaultPageSize(int defaultPageSize) {
            this.defaultPageSize = defaultPageSize;
        }

        public int getMaxPageSize() {
            return maxPageSize;
        }

        public void setMaxPageSize(int maxPageSize) {
            this.maxPageSize = maxPageSize;
        }
    }
}
```

This binds all properties under the `bookshop` prefix into Java objects.

## 6. Use the config in the controller: ShopController.java

```java
package com.example.bookshop.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bookshop.config.BookshopProperties;
import com.example.bookshop.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/shop")
public class ShopController {

    private final BookshopProperties properties;

    public ShopController(BookshopProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public ApiResponse<Map<String, String>> getShopInfo() {
        return ApiResponse.ok("Shop info fetched", Map.of(
                "name", properties.getShopName(),
                "currency", properties.getCurrency()));
    }
}
```

The controller receives configuration through constructor injection.

## 7. Why this matters

This tutorial is about making configuration:

- external to code
- typed and validated
- profile-aware
- overridable by environment variables

## 8. Common mistakes

- wrong profile filename such as `application_dev.properties`
- forgetting `@ConfigurationPropertiesScan`
- using secrets directly in source-controlled files
- misspelling a property key

## 9. Goal for this tutorial

By the end of this tutorial, you should understand:

- how config is organized in Spring Boot
- how the `bookshop.*` keys map into Java objects
- how dev and prod profiles work
- how environment variables override defaults

Next: [**Tutorial 05 — Database and JPA**](tutorial-05%20%28Database%20and%20JPA%29.md)
