# Tutorial 20 — The template

Turning the course project into the starter you copy for every new
backend.

---

## 1. What this repo is now

Twenty stages later, `src/` holds a complete, production-shaped API
and `docs/` holds the course that explains every line. Both are the
deliverable: read the docs to relearn a topic, copy the code to
start a project. (The same dual role as node-mongo-helper.)

Housekeeping done this stage: `HELP.md` (generator link list)
deleted; the README rewritten as the real index; the repo put under
git with its first commit.

## 2. Copying it for a new project

Say the new project is an inventory service, package
`com.example.inventory`:

```bash
# 1. copy, without this repo's git history
git clone <this-repo-url> inventory && cd inventory
rm -rf .git

# 2. rename the Maven artifact: in pom.xml change
#    <artifactId>, <name>, <description> (keep the parent!)

# 3. re-package (IDE refactor-rename is easiest: right-click the
#    com.example.bookshop package -> Rename). By hand it is:
#    - move src/main/java/com/example/bookshop -> .../inventory
#    - same under src/test/java
#    - find & replace "com.example.bookshop" -> "com.example.inventory"
#    - rename BookshopApplication -> InventoryApplication (+ its test)

# 4. rename the config prefix: BookshopProperties' prefix "bookshop"
#    and every "bookshop." key in the .properties files

# 5. empty the domain: delete Book/Author/Genre (entities, repos,
#    services, controllers, DTOs) and db/migration/V2__seed_catalog.sql;
#    rewrite V1__init.sql for the new domain. KEEP: user_account +
#    everything in config/, exception/, dto/ApiResponse+ErrorResponse+
#    PageMeta, AuthService/AuthController - that is the template.

# 6. fresh start
./mvnw test && git init && git add . && git commit -m "Initial commit from bookshop template"
```

What carries over untouched to any project: the envelope + error
handling, auth (register/login/JWT/roles/seeder), security config,
rate limiting, CORS, logging setup, Flyway wiring, Actuator,
Dockerfile, compose.yaml, the test structure, and every convention.

## 3. The decision log (why things are the way they are)

```
+--------------------------------+-----------------------------------+
| Decision                       | Where argued                      |
+--------------------------------+-----------------------------------+
| Maven wrapper, never local mvn | tut. 01                           |
| /api/v1 prefix everywhere      | tut. 02                           |
| constructor injection, final   | tut. 03                           |
| typed @ConfigurationProperties,| tut. 04                           |
| fail-fast validation           |                                   |
| BigDecimal for money           | tut. 02/05                        |
| DTO records at the boundary    | tut. 07 (the costPrice leak)      |
| one envelope, one error shape  | tut. 07/09                        |
| whitelists over blacklists     | tut. 10 (sort/filter), 15 (URLs)  |
| everything LAZY + fetch per    | tut. 10 (the N+1 capture)         |
| query                          |                                   |
| soft delete via @SoftDelete    | tut. 11                           |
| @Transactional on service      | tut. 12 (the half-applied         |
| methods that write twice       | discount)                         |
| identity as a method parameter | tut. 16 (unit-testable ownership) |
| role read from DB, not token   | tut. 16                           |
| Flyway owns schema, Hibernate  | tut. 18 (the bytea bug)           |
| validates, same scripts dev+   |                                   |
| prod                           |                                   |
| env vars complete the jar;     | tut. 04/19                        |
| missing config = refuse to boot|                                   |
+--------------------------------+-----------------------------------+
```

## 4. If you want Lombok later

We wrote getters/setters by hand so nothing was hidden while
learning. Lombok generates them at compile time; most industry
codebases use it. To switch: add the `lombok` dependency (and the
annotation-processor block Lombok's site shows for Maven), install
the IDE plugin, then replace boilerplate with `@Getter @Setter`
`@NoArgsConstructor(access = PROTECTED)` on entities and drop it
entirely for records (records already need nothing). Do it in one
commit, run `./mvnw test`, and read the generated-code warnings
honestly — Lombok on ENTITIES has sharp edges (`@Data`'s equals/
hashCode and toString can trigger lazy loading; use the narrow
annotations, never `@Data`, on entities).

## 5. What a real project adds next

Honest gaps, in the order they usually start to hurt: refresh
tokens (ours expire in 60 min, full stop), request correlation ids
in logs (MDC), Testcontainers so the test suite runs against real
Postgres (needs Docker), CI (run `./mvnw test` on every push),
metrics (Actuator + Prometheus), and DB-backed rate limiting when
you run more than one instance.

## 6. The first commit

```bash
git init
git add .
git commit -m "Bookshop API - 20-stage Spring Boot course and starter template"
```

Then create an empty repo on GitHub and:

```bash
git remote add origin git@github.com:<you>/<repo>.git
git branch -M main
git push -u origin main
```

(The push needs your credentials, so it is yours to run.)

## 7. The course, in one paragraph

A framework calls your code. Beans live in a container and arrive
through constructors. The web layer speaks HTTP and DTOs; services
own rules and transactions; repositories own SQL — generated,
inspected in the log, and fetch-planned per query. Everything that
leaves is one envelope; everything that enters is validated at the
door and identified by a signed token. Configuration completes the
app from outside, schema changes are numbered files, and every
claim gets verified by running it — because the log, the database
and the status code do not care what the code was supposed to do.
