# Tutorial 18 — A real database

PostgreSQL, Flyway migrations, and retiring create-drop + data.sql.

Files for this stage:
- `src/main/resources/db/migration/V1__init.sql`,
  `V2__seed_catalog.sql` (new — Flyway owns the schema now)
- `pom.xml` (+ postgresql driver, spring-boot-starter-flyway,
  flyway-database-postgresql)
- `compose.yaml` (new — Postgres via Docker)
- `application.properties` (ddl-auto=validate; data.sql DELETED)
- `application-prod.properties` (datasource from env vars)
- `repository/BookRepository.java` (one Postgres-only bug fixed)

> **Honesty note on verification:** Docker is not installed on this
> machine, so everything below was verified against a REAL
> PostgreSQL 17.11 installed via Homebrew (same server, different
> launcher). The `compose.yaml` path is the standard one for you to
> use — install Docker Desktop before following it.

---

## 1. Why H2 must retire (and why create-drop must too)

H2-in-memory was perfect for learning: zero setup, fresh every
start. Those same properties disqualify it for production — data
that dies with the process is not a database. And `ddl-auto`
= `create-drop` let Hibernate invent the schema from our entities,
which fails the moment real data exists: you cannot drop-and-create
a table with customers in it, and "Hibernate guessed a column type"
is not something to discover during a deploy.

The grown-up arrangement:

```
+---------------------+---------------------------------------------+
| Flyway              | OWNS the schema: numbered SQL scripts,      |
|                     | applied once each, in order, tracked        |
| Hibernate           | ddl-auto=validate - only CHECKS that        |
|                     | entities and tables still agree             |
| PostgreSQL          | the real database (prod)                    |
| H2                  | still used for dev/tests - but built by     |
|                     | the SAME Flyway scripts as prod             |
+---------------------+---------------------------------------------+
```

> A **migration** is a numbered, immutable SQL script:
> `V1__init.sql`, `V2__seed_catalog.sql`, ... Flyway keeps a table
> (`flyway_schema_history`) recording which have run, and applies
> only the new ones. Never edit an applied migration — write the
> next one. (Same idea git has about published commits.)

## 2. What changed

**V1__init.sql** — the whole schema, hand-written, in portable SQL
that runs on both H2 and Postgres. Improvements over the generated
schema: real foreign keys, indexes on `book.author_id` and
`book.owner_id`, and `deleted BOOLEAN NOT NULL DEFAULT FALSE` — the
default fixes tutorial 11's seeding pain at the root.

**V2__seed_catalog.sql** — the old `data.sql` content, now a
migration (and thanks to the DEFAULT, it never mentions `deleted`).
`data.sql` itself is deleted; `defer-datasource-initialization` went
with it. The admin user still comes from `AdminSeeder` — it needs a
BCrypt hash, which SQL cannot make.

**Boot 4 gotcha, found by running:** with only `flyway-core` in the
pom, the app failed with `Schema validation: missing table [author]`
— migrations never ran, no error about it. Boot 4 moved Flyway's
auto-configuration into `spring-boot-starter-flyway`. With the
starter (verified):

```
Migrating schema "PUBLIC" to version "1 - init"
Migrating schema "PUBLIC" to version "2 - seed catalog"
Successfully applied 2 migrations to schema "PUBLIC"
books via Flyway schema: 5
```

That was H2 — dev now boots from the same scripts as prod.

## 3. Postgres for real

The standard path (needs Docker Desktop):

```bash
docker compose up -d        # starts Postgres 17 from compose.yaml
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bookshop \
SPRING_DATASOURCE_USERNAME=bookshop \
SPRING_DATASOURCE_PASSWORD=bookshop-local-pw \
./mvnw spring-boot:run
```

`compose.yaml` describes the container: image `postgres:17`, a named
volume so data survives restarts, a healthcheck, local-only fake
credentials. In prod the app reads `DB_URL` / `DB_USER` /
`DB_PASSWORD` from the environment with NO fallbacks (fail fast).

Verified against the real server (Homebrew Postgres 17.11):

```
Flyway: Database: jdbc:postgresql://localhost:5432/bookshop (PostgreSQL 17.11)
        Successfully applied 2 migrations to schema "public"

psql> select id, title, price, deleted from book order by id limit 3;
 id |          title           | price | deleted
----+--------------------------+-------+---------
  1 | Effective Java           | 54.99 | f
  2 | Clean Code               | 42.50 | f
  3 | The Pragmatic Programmer | 49.95 | f

psql> select version, description, success from flyway_schema_history;
 1 | init         | t
 2 | seed catalog | t

register on Postgres -> 201     (BCrypt, timestamptz - all fine)
```

## 4. The bug H2 was hiding

First list request against Postgres:

```
GET /api/v1/books -> 500
ERROR: function lower(bytea) does not exist
```

Months of H2 runs, tests, curls — all green. Postgres, first
request, boom. The cause: our search query's
`(:search is null or lower(:search)...)` pattern sends `:search` as
NULL, Postgres must assign every parameter a type, guesses `bytea`
(raw bytes), and `lower(bytea)` does not exist. The fix pins the
type in the query:

```java
lower(concat('%', cast(:search as string), '%'))
```

After the cast, every query feature verified on Postgres: plain
list, search, price range, genre filter, author + sort — all
correct, suite still 13/13 on H2.

Two lessons bigger than the bug: **"works on my database" is a real
category of bug** — dev/prod parity (same engine everywhere, which
`compose.yaml` now makes cheap) exists to catch it; and every
database has personality — H2 is forgiving, Postgres is precise.

## 5. The common mistakes

1. **Editing an applied migration.** Flyway checksums every script;
   editing V1 after it ran fails startup with a checksum error on
   every machine that already applied it. New change = new file.
2. **flyway-core without the Boot 4 starter** — migrations silently
   don't run; the first symptom is Hibernate's "missing table".
3. **Leaving ddl-auto on create/update alongside Flyway** — two
   owners of one schema; they will disagree. Flyway migrates,
   Hibernate validates. One writer.
4. **Testing only on H2 forever** — see the bytea story above.

## 6. Recap

- Flyway owns the schema: numbered, immutable, tracked scripts —
  the same ones for H2 dev and Postgres prod.
- `ddl-auto=validate`: Hibernate is now a schema *auditor*, not an
  author. data.sql retired into V2.
- Postgres runs in Docker via `compose.yaml`; the app finds it
  through environment variables; prod has no fallbacks.
- Real-database testing found a real bug the friendly database hid.

Next: [**Tutorial 19 — Production basics**](tutorial-19%20%28Production%20basics%29.md): Actuator health checks,
building the jar, a Dockerfile, graceful shutdown, and configuration
through environment variables.
