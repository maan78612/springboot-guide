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
| 01 | [tutorial-01 (Setup)](docs/tutorial-01%20%28Setup%29.md) | what Spring Boot is, the generated files, pom.xml, running, dev reload |
| 02 | [tutorial-02 (First endpoint)](docs/tutorial-02%20%28First%20endpoint%29.md) | @RestController, JSON out, /api/v1 |
| 03 | [tutorial-03 (Layers and dependency injection)](docs/tutorial-03%20%28Layers%20and%20dependency%20injection%29.md) | beans, the container, constructor injection |
| 04 | [tutorial-04 (Configuration)](docs/tutorial-04%20%28Configuration%29.md) | properties, profiles, @ConfigurationProperties, fail-fast, secrets |
| 05 | [tutorial-05 (Database and JPA)](docs/tutorial-05%20%28Database%20and%20JPA%29.md) | H2, @Entity, JpaRepository, seeing the SQL |
| 06 | [tutorial-06 (Full CRUD API)](docs/tutorial-06%20%28Full%20CRUD%20API%29.md) | verbs, @PathVariable, @RequestBody, status codes |
| 07 | [tutorial-07 (DTOs and the response envelope)](docs/tutorial-07%20%28DTOs%20and%20the%20response%20envelope%29.md) | records, mapping, the envelope |
| 08 | [tutorial-08 (Validation)](docs/tutorial-08%20%28Validation%29.md) | @Valid, constraints, field errors |
| 09 | [tutorial-09 (Error handling)](docs/tutorial-09%20%28Error%20handling%29.md) | ApiException, @RestControllerAdvice, one error shape |
| 10 | [tutorial-10 (Relationships and queries)](docs/tutorial-10%20%28Relationships%20and%20queries%29.md) | OneToMany/ManyToMany, N+1, derived queries, @Query, query features |
| 11 | [tutorial-11 (Soft delete)](docs/tutorial-11%20%28Soft%20delete%29.md) | @SoftDelete, restore, native-query escape hatches |
| 12 | [tutorial-12 (Transactions)](docs/tutorial-12%20%28Transactions%29.md) | @Transactional, rollback rules, lazy traps |
| 13 | [tutorial-13 (Testing)](docs/tutorial-13%20%28Testing%29.md) | Mockito, @WebMvcTest, @SpringBootTest |
| 14 | [tutorial-14 (Logging)](docs/tutorial-14%20%28Logging%29.md) | SLF4J, levels, dev-pretty vs prod-JSON |
| 15 | [tutorial-15 (Auth)](docs/tutorial-15%20%28Auth%29.md) | security defaults, BCrypt, JWT, seed admin |
| 16 | [tutorial-16 (Roles and hardening)](docs/tutorial-16%20%28Roles%20and%20hardening%29.md) | roles, ownership, CORS, rate limit, headers |
| 17 | [tutorial-17 (API documentation)](docs/tutorial-17%20%28API%20documentation%29.md) | OpenAPI, Swagger UI, Postman |
| 18 | [tutorial-18 (A real database)](docs/tutorial-18%20%28A%20real%20database%29.md) | Postgres, Flyway, dev/prod parity |
| 19 | [tutorial-19 (Production basics)](docs/tutorial-19%20%28Production%20basics%29.md) | Actuator, jar, Dockerfile, env config, graceful shutdown |
| 20 | [tutorial-20 (The template)](docs/tutorial-20%20%28The%20template%29.md) | using this repo as your starter |

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

See [tutorial-20 (The template)](docs/tutorial-20%20%28The%20template%29.md) — clone, rename, re-package,
`git init`, go.
