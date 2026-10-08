# Tutorial 20 — The template

Turn this project into a repeatable backend starter you can copy for every new Spring Boot service.

---

## 1. What This Repo Is Now

Twenty stages later, `src/` holds a complete, production-grade Spring Boot API and `docs/` holds the course that explains every architectural decision. Both serve as deliverables: read the documentation to understand concepts, and copy the repository as a starter template for new services.

### What Carries Over Untouched
The reusable parts are the core infrastructure:
- **API Envelope & Error Handling**: `ApiResponse<T>`, `ErrorResponse`, `PageMeta`, `ApiException`, and `GlobalExceptionHandler`.
- **Security & Identity**: JWT generation, BCrypt hashing, stateless `SecurityFilterChain`, `UserAccount`, and `AdminSeeder`.
- **Hardening**: Rate limiting, CORS configuration, security headers, and `@PreAuthorize` method security.
- **Data & Persistence**: Flyway database migrations, PostgreSQL configuration, and soft-delete patterns.
- **Operations & DevOps**: Structured JSON logging, Actuator health checks, multi-stage `Dockerfile`, and `compose.yaml`.
- **Testing Structure**: Mockito unit tests, `@WebMvcTest` controller slices, and `@SpringBootTest` full integration tests.

---

## 2. How to Start a New Project from this Template

To create a new service (e.g. an inventory service in `com.example.inventory`):

```bash
# 1. Clone the repository into a new directory
git clone <this-repo-url> inventory && cd inventory
rm -rf .git

# 2. Rename the Maven artifact in pom.xml:
#    Update <artifactId>, <name>, and <description>

# 3. Rename package directory structure:
#    - Move src/main/java/com/example/bookshop -> src/main/java/com/example/inventory
#    - Move src/test/java/com/example/bookshop -> src/test/java/com/example/inventory
#    - Find and replace "com.example.bookshop" -> "com.example.inventory"
#    - Rename BookshopApplication.java -> InventoryApplication.java

# 4. Rename configuration prefixes:
#    - Change prefix in BookshopProperties from "bookshop" to "inventory"
#    - Update property keys in application*.properties

# 5. Clear domain-specific models:
#    - Remove Book, Author, and Genre (entities, repositories, services, controllers, DTOs).
#    - Rewrite V1__init.sql for your new business tables.
#    - Keep: user_account, config/, exception/, dto/ApiResponse, AuthService, AuthController.

# 6. Verify and initialize git
./mvnw clean test
git init
git add .
git commit -m "Initial commit from backend template"
```

---

## 3. The Architectural Decision Log

A recap of key engineering choices made across the 20 tutorials:

| Decision | Why We Decided It | Tutorial Reference |
| :--- | :--- | :--- |
| **Maven Wrapper (`./mvnw`)** | Guarantees identical Maven runtime without requiring developer installations. | Tutorial 01 |
| **`/api/v1` URL Prefix** | Enables backward-compatible API versioning. | Tutorial 02 |
| **Constructor Injection & `final` Fields** | Guarantees non-null dependencies and allows fast unit testing with plain `new`. | Tutorial 03 |
| **Typed `@ConfigurationProperties`** | Validates configuration types at startup rather than throwing runtime errors. | Tutorial 04 |
| **`BigDecimal` for Money** | Eliminates binary floating-point rounding errors on currency calculations. | Tutorial 02 / 05 |
| **DTO Records at the Boundary** | Prevents internal fields (e.g. `costPrice`) from leaking out of the API. | Tutorial 07 |
| **Unified Envelope (`ApiResponse`)** | Ensures frontends receive predictable response shapes for both success and failure. | Tutorial 07 / 09 |
| **Whitelists over Blacklists** | Explicitly limits sorting fields and open URLs to approved values. | Tutorial 10 / 15 |
| **`FetchType.LAZY` + Fetch Joins** | Avoids silent N+1 queries by fetching relationships deliberately. | Tutorial 10 |
| **Soft Delete (`@SoftDelete`)** | Preserves historical audit records and avoids foreign key cascade deletions. | Tutorial 11 |
| **`@Transactional` on Services** | Coordinates multi-query updates as atomic, all-or-nothing transactions. | Tutorial 12 |
| **Identity as a Method Parameter** | Avoids hidden static context calls, making ownership rules easy to unit test. | Tutorial 16 |
| **Roles Checked from DB** | Ensures revoked or changed user roles take effect on the next request. | Tutorial 16 |
| **Flyway Schema Migrations** | Replaces ephemeral `create-drop` with versioned, reproducible SQL files. | Tutorial 18 |
| **Environment Variable Config** | Keeps container images environment-agnostic; fails fast if config is missing. | Tutorial 04 / 19 |

---

## 4. If You Want Project Lombok Later

We wrote constructors, getters, and setters by hand so nothing was hidden while learning. To adopt Lombok later:
1. Add `org.projectlombok:lombok` to `pom.xml`.
2. Replace boilerplate in JPA entities with `@Getter`, `@Setter`, and `@NoArgsConstructor(access = AccessLevel.PROTECTED)`.
3. Keep DTOs as Java `record` types (records need no Lombok annotations).

> [!CAUTION]
> **Avoid `@Data` on JPA Entities**:
> Lombok's `@Data` generates automatic `equals()`, `hashCode()`, and `toString()` methods that inspect every field. On JPA entities with `@ManyToMany` or lazy associations, this can trigger unintended database queries or fatal `StackOverflowError` loops. Use narrow `@Getter` and `@Setter` annotations instead.

---

## 5. What a Real Project Adds Next

As your application grows in scale, consider adding:
1. **Refresh Tokens**: Issue short-lived access tokens (15 minutes) paired with revocable database-backed refresh tokens.
2. **Distributed Tracing & Correlation IDs**: Use MDC (Mapped Diagnostic Context) to attach a unique `requestId` to every log entry across distributed microservices.
3. **Testcontainers**: Run integration test suites against disposable Docker containers running real PostgreSQL rather than H2.
4. **Distributed Rate Limiting**: Store request throttles in Redis when scaling out to multiple load-balanced instances.

---

## 6. The Entire Course, in One Paragraph

A framework calls your code. Beans live in an IoC container and arrive through constructors. The web layer speaks HTTP and DTOs; services own business logic and transaction boundaries; repositories own SQL — inspected in debug logs and fetch-planned per query. Everything leaving the API follows a standard envelope; everything entering is validated at the boundary and identified by a cryptographically signed token. Configuration completes the application from the outside, database migrations are versioned scripts, and every behavior is verified through automated tests.

This project is now a practical starter template, not just a tutorial app.
