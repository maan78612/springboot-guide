# Tutorial 04 — Configuration

Move the shop name and currency out of Java code and into Spring configuration.

Why configuration matters: application settings change between environments and teams, but the Java code should not need to be edited each time. We keep environment-specific values outside the code so the same app can run in development, test, and production with different URLs, credentials, feature flags, and limits.

Common use cases for configuration:

- different database URLs and credentials per environment
- secret keys, JWT settings, and admin defaults
- feature toggles for turning parts of the app on or off
- server ports, timeouts, and cache settings
- external service endpoints such as payment or email providers

Files used in this tutorial:

- Updated: `src/main/resources/application.properties`
- New: `src/main/resources/application-dev.properties`
- New: `src/main/resources/application-prod.properties`
- New: `src/main/java/com/example/bookshop/config/BookshopProperties.java`
- New: `src/main/java/com/example/bookshop/controller/ShopController.java`
- Updated: `src/main/java/com/example/bookshop/BookshopApplication.java`

---

## 1. Add the default values

Add these settings to `application.properties`:

```properties
bookshop.shop-name=Bookshop
bookshop.currency=USD
```

The `bookshop` prefix groups the settings that belong to this application.

## 2. Add profile-specific overrides

In `application-dev.properties`, override the shop name for development:

```properties
bookshop.shop-name=Bookshop (dev)
```

In `application-prod.properties`, allow the deployed environment to provide the name:

```properties
bookshop.shop-name=${BOOKSHOP_SHOP_NAME:Bookshop}
```

Activate a profile when starting the app:

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Spring loads the base properties first, then applies the active profile's overrides. Environment variables can also override properties, for example `BOOKSHOP_CURRENCY=EUR`.

## 3. Bind the settings to a Java class

Create `BookshopProperties.java`:

```java
package com.example.bookshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bookshop")
public class BookshopProperties {

    private String shopName;
    private String currency;

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
}
```

Spring maps `bookshop.shop-name` to `shopName` and `bookshop.currency` to `currency`.

Why a config package: the application settings belong in one place, not scattered across controllers, services, and startup classes. A dedicated config package keeps the settings organized, easy to read, and easy to reuse in multiple parts of the app.

## 4. Register the configuration class

In `BookshopApplication.java`, add the import and annotation to the existing app class:

```java
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BookshopApplication {
    // Keep the existing main method.
}
```

This makes `BookshopProperties` available for injection.

## 5. Read the settings in a controller

Create `ShopController.java`:

```java
package com.example.bookshop.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bookshop.config.BookshopProperties;

@RestController
@RequestMapping("/api/v1/shop")
public class ShopController {

    private final BookshopProperties properties;

    public ShopController(BookshopProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public Map<String, String> getShopInfo() {
        return Map.of(
                "name", properties.getShopName(),
                "currency", properties.getCurrency());
    }
}
```

Run the app and request `GET /api/v1/shop` to see the configured values.

## 6. Common mistakes

- Naming a profile file `application_dev.properties` instead of `application-dev.properties`.
- Forgetting `@ConfigurationPropertiesScan`.
- Misspelling a property key.

Next: [**Tutorial 05 — Database and JPA**](tutorial-05%20%28Database%20and%20JPA%29.md)
