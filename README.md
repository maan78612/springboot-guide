# Bookshop API — a Spring Boot course and starter template

One Spring Boot project built in 20 numbered stages. Each stage is a
tutorial in [docs/](docs/) plus real, runnable code — read the
course front to back, or copy patterns from the finished template.

This is the Java twin of the
[node-mongo-helper](https://github.com/maan78612/node-mongo-helper)
reference: same layered architecture, same response envelope, same
feature set — a client can consume either backend with the same code.

The domain: a small bookshop marketplace. Sellers register and list
books; admins manage everything.

## Features

- Layered architecture: controller → service → repository, DTOs at
  the boundary (records), entities never leave the service layer
- One response envelope everywhere:
  `{"success", "message", "data", "meta"}` — errors:
  `{"success": false, "message", "errors": [...]}`
- JWT auth (register/login, BCrypt hashing), user/admin roles,
  ownership checks (sellers modify only their own books)
- Full CRUD + pagination, whitelisted search / filter / sort with
  pagination `meta`
- Soft delete with admin restore (Hibernate `@SoftDelete`)
- Validation with field-level error details; one global error
  handler; consistent status codes
- Flyway migrations (same scripts for H2 dev and Postgres prod),
  `ddl-auto=validate`
- Rate-limited auth endpoints, CORS from config, secure headers
- OpenAPI + Swagger UI (dev), importable into Postman
- Structured JSON logs in prod, pretty logs + SQL echo in dev
- Actuator health for load balancers, graceful shutdown, Dockerfile
- Tests at three levels: Mockito units, @WebMvcTest slice,
  @SpringBootTest end-to-end

## Requirements

- Java 25 (`java -version`). Maven NOT required — `./mvnw` fetches it.
- Docker Desktop only for running Postgres locally (tutorial 18+).

## Run (dev)

```bash
./mvnw spring-boot:run                          # H2, port 8080
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run   # + SQL logs, H2 console, dev admin
```

Smoke test:

```bash
curl http://localhost:8080/api/v1/books
curl http://localhost:8080/actuator/health
```

Dev conveniences (dev profile): Swagger UI at
http://localhost:8080/swagger-ui/index.html, H2 console at
http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:bookshop`,
user `sa`, empty password), seeded admin `admin@bookshop.local` /
`admin-dev-password` (fake, dev-only).

Tests: `./mvnw test`. Package: `./mvnw clean package` →
`java -jar target/bookshop-0.0.1-SNAPSHOT.jar`.

## Run (prod shape)

```bash
docker compose up -d          # local Postgres 17
SPRING_PROFILES_ACTIVE=prod \
JWT_SECRET=<32+ chars> \
DB_URL=jdbc:postgresql://localhost:5432/bookshop \
DB_USER=bookshop DB_PASSWORD=bookshop-local-pw \
ADMIN_EMAIL=<email> ADMIN_PASSWORD=<password> \
java -jar target/bookshop-0.0.1-SNAPSHOT.jar
```

Required environment (prod refuses to start without the first four):

| Variable | Purpose |
|---|---|
| `JWT_SECRET` | token signing key, 32+ characters |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | PostgreSQL connection |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | first-admin seed (optional; blank = skip) |

## The tutorials

| #  | File in docs/ | Covers |
|----|------|--------|
| 01 | tutorial-01 (Setup) | what Spring Boot is, the generated files, pom.xml, running, dev reload |
| 02 | tutorial-02 (First endpoint) | @RestController, JSON out, /api/v1 |
| 03 | tutorial-03 (Layers and dependency injection) | beans, the container, constructor injection |
| 04 | tutorial-04 (Configuration) | properties, profiles, @ConfigurationProperties, fail-fast, secrets |
| 05 | tutorial-05 (Database and JPA) | H2, @Entity, JpaRepository, seeing the SQL |
| 06 | tutorial-06 (Full CRUD API) | verbs, @PathVariable, @RequestBody, status codes |
| 07 | tutorial-07 (DTOs and the response envelope) | records, mapping, the envelope |
| 08 | tutorial-08 (Validation) | @Valid, constraints, field errors |
| 09 | tutorial-09 (Error handling) | ApiException, @RestControllerAdvice, one error shape |
| 10 | tutorial-10 (Relationships and queries) | OneToMany/ManyToMany, N+1, derived queries, @Query, query features |
| 11 | tutorial-11 (Soft delete) | @SoftDelete, restore, native-query escape hatches |
| 12 | tutorial-12 (Transactions) | @Transactional, rollback rules, lazy traps |
| 13 | tutorial-13 (Testing) | Mockito, @WebMvcTest, @SpringBootTest |
| 14 | tutorial-14 (Logging) | SLF4J, levels, dev-pretty vs prod-JSON |
| 15 | tutorial-15 (Auth) | security defaults, BCrypt, JWT, seed admin |
| 16 | tutorial-16 (Roles and hardening) | roles, ownership, CORS, rate limit, headers |
| 17 | tutorial-17 (API documentation) | OpenAPI, Swagger UI, Postman |
| 18 | tutorial-18 (A real database) | Postgres, Flyway, dev/prod parity |
| 19 | tutorial-19 (Production basics) | Actuator, jar, Dockerfile, env config, graceful shutdown |
| 20 | tutorial-20 (The template) | using this repo as your starter |

Every claim in the docs was verified by running it; error messages
shown are real output.

## Conventions

Notes live in the tutorial docs and in a `/* */` block at the top of
each `.java` file, using Better Comments markers:

```
^  a heading
?  a fact, a definition, a list of options
!  a warning, a mistake, something that will not compile
+  the correct way to do it
*  ordinary prose
```

## Using this as a template

See `docs/tutorial-20 (The template).md` — clone, rename, re-package,
`git init`, go.
