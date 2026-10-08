# Tutorial 17 — API documentation

Generate interactive API documentation directly from code. Learn how SpringDoc OpenAPI automatically inspects Spring MVC controllers and validation annotations to generate live OpenAPI 3 specs, Swagger UI, and Postman collections.

Files for this stage:

- Updated: `pom.xml` (`springdoc-openapi-starter-webmvc-ui`)
- New: `src/main/java/com/example/bookshop/config/OpenApiConfig.java`
- Updated: `src/main/java/com/example/bookshop/config/SecurityConfig.java` (permit Swagger URLs)
- Updated: `src/main/resources/application-prod.properties` (disable docs in production)

---

## 1. Why Generated API Documentation?

Hand-written documentation (wikis, Word documents, static markdown tables) always falls out of sync with code: someone renames a field, and the document is instantly stale.

> **OpenAPI**: A vendor-neutral, machine-readable JSON/YAML specification format describing all HTTP paths, parameters, schemas, and status codes.
> **SpringDoc**: A library that analyzes Spring MVC `@RestController` methods, `@Valid` constraints, and Jackson DTO records at application startup to build the OpenAPI specification dynamically.
> **Swagger UI**: A browser-based interactive dashboard rendered directly from the OpenAPI specification, featuring live "Try it out" execution buttons.

---

## 2. Add OpenAPI and Swagger UI

Add this dependency inside the `<dependencies>` section of `pom.xml`:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.8</version>
</dependency>
```

| Key / Dependency | Why we use it |
| :--- | :--- |
| `springdoc-openapi-starter-webmvc-ui` | Generates the OpenAPI document and Swagger UI |
| Explicit `<version>` | SpringDoc is a third-party project (`org.springdoc`), so its version must be pinned explicitly rather than inherited from Spring Boot's parent BOM. |

This generates the API spec and a browser UI automatically.

---

## 3. Configure Security for Swagger UI

When Spring Security is active, `anyRequest().authenticated()` blocks newly introduced library routes by default. You will see:
```text
GET /swagger-ui/index.html -> 401 Unauthorized
```

To resolve this, explicitly permit documentation paths in `SecurityConfig.java`:

```java
.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
```

---

## 4. Configure `OpenApiConfig` with Bearer Authentication

Create `src/main/java/com/example/bookshop/config/OpenApiConfig.java`. This configures the API title and adds the `bearerAuth` security scheme so Swagger UI displays an interactive **Authorize** button:

```java
package com.example.bookshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI bookshopOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bookshop API")
                        .version("v1.0")
                        .description("REST API for Bookshop catalog and order management"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
```

---

## 5. Open the Docs and Test Interactively

Start your application and navigate to:

```text
GET /v3/api-docs
GET /swagger-ui/index.html
```

Swagger UI shows the real API contract from the current code.

| Endpoint | Purpose |
| :--- | :--- |
| `/v3/api-docs` | Serves the generated OpenAPI JSON specification. |
| `/swagger-ui/index.html` | Opens the interactive API documentation dashboard. |

### How to Authenticate in Swagger UI
1. Scroll down to `POST /api/v1/auth/login`.
2. Click **Try it out** and execute with valid credentials.
3. Copy the returned `token` string.
4. Scroll to the top of Swagger UI and click the **Authorize 🔓** button.
5. Paste your token and click **Authorize**.
6. Every subsequent request made from the browser will include the `Authorization: Bearer <token>` header!

---

## 6. One-Click Import into Postman

You do not need to manually maintain Postman collections:
1. Open Postman -> Click **Import**.
2. Paste your local API docs URL: `http://localhost:8080/v3/api-docs`.
3. Postman automatically creates a fully populated collection containing every endpoint, complete with request schemas and path parameters.

---

## 7. Disabling Swagger in Production

While invaluable in development, public-facing internal APIs should disable interactive documentation in production to minimize attack surface area. In `src/main/resources/application-prod.properties`:

```properties
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false
```

Next: [**Tutorial 18 — A real database**](tutorial-18%20%28A%20real%20database%29.md)
