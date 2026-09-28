# Tutorial 17 — API documentation

Generate the API contract from the code.

Files for this stage:

- Updated: `pom.xml`
- New: `src/main/java/com/example/bookshop/config/OpenApiConfig.java`
- Updated: `src/main/java/com/example/bookshop/config/SecurityConfig.java`

---

## 1. Add OpenAPI and Swagger UI

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.8</version>
</dependency>
```

| Key / Dependency                    | Why we use it                                 |
| ----------------------------------- | --------------------------------------------- |
| springdoc-openapi-starter-webmvc-ui | Generates the OpenAPI document and Swagger UI |

This generates the API spec and a browser UI automatically.

## 2. Open the docs

```text
GET /v3/api-docs
GET /swagger-ui/index.html
```

Swagger UI shows the real API contract from the current code.

| Endpoint                 | Purpose                                         |
| ------------------------ | ----------------------------------------------- |
| `/v3/api-docs`           | Serves the generated OpenAPI JSON specification |
| `/swagger-ui/index.html` | Opens the interactive API documentation         |

Next: [**Tutorial 18 — A real database**](tutorial-18%20%28A%20real%20database%29.md)
