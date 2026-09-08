# Tutorial 19 — Production basics

Actuator health checks, building the jar, a Dockerfile, graceful
shutdown, and configuration through environment variables.

Files for this stage:
- `pom.xml` (+ spring-boot-starter-actuator)
- `config/SecurityConfig.java` (/actuator/health opened)
- `application-dev.properties` (health details in dev)
- `application-prod.properties` (graceful shutdown)
- `Dockerfile`, `.dockerignore` (new)

---

## 1. Health checks: how machines ask "are you okay?"

Load balancers, Kubernetes, uptime monitors — they all poll an HTTP
endpoint and route traffic away when it stops saying yes. Spring's
**Actuator** starter provides it (plus many other operational
endpoints) with zero code:

```
GET /actuator/health   ->  {"groups":["liveness","readiness"],"status":"UP"}
```

Wired into our security chain: `/actuator/health` is public (probes
don't log in); every OTHER actuator endpoint falls under
`anyRequest().authenticated()` — verified: `/actuator/env` answers
401. Endpoints like env and heapdump print your configuration and
memory; treat them as admin tools.

In dev, `management.endpoint.health.show-details=always` shows the
components — db, diskSpace, ping, liveness/readiness — so you see
WHY something is down.

**Observed live, worth knowing:** polling health in the first
second of startup returned `{"status":"OUT_OF_SERVICE"} | 503` —
the readiness probe correctly says "not yet" until startup tasks
(our AdminSeeder included) finish. That 503 is a feature: it is
what stops a load balancer from routing to a half-started instance.

## 2. The jar: one file, everything inside

```bash
./mvnw clean package
# -> target/bookshop-0.0.1-SNAPSHOT.jar   (66 MB, verified)
java -jar target/bookshop-0.0.1-SNAPSHOT.jar
```

Tomcat, all dependencies, our classes and migrations — one file.
This is what tutorial 01 promised: no server to install, the server
is IN the app. Deployment is "copy one file, run java".

## 3. Configuration through the environment (the payoff run)

The SAME jar must run anywhere; only the environment differs
(tutorial 04's precedence rules doing their production job). Both
directions verified with the real jar:

**Refuses to start incomplete.** With `prod` active and no env vars:
exit code 1 — `BeanCreationException` on datasource init, because
`${DB_URL}` and friends have no fallback. A prod instance missing
its configuration must die loudly at startup, not limp.

**The full production run.** Real Postgres running, then:

```bash
SPRING_PROFILES_ACTIVE=prod SERVER_PORT=8081 \
JWT_SECRET=... DB_URL=jdbc:postgresql://localhost:5432/bookshop \
DB_USER=... DB_PASSWORD=... ADMIN_EMAIL=... ADMIN_PASSWORD=... \
java -jar target/bookshop-0.0.1-SNAPSHOT.jar
```

Everything this course built, verified in one process:

```
health   -> {"status":"UP"} | 200          (db component = Postgres)
books    -> ['Effective Java', 'Clean Code'] total 5
swagger  -> 404                            (disabled in prod, tut. 17)
flyway   -> "up to date. No migration necessary"
                                           (already-migrated db: no-op, by design)
logs     -> {"@timestamp":"...","log":{"level":"INFO",...},
             "message":"Tomcat started on port 8081..."}   (ECS JSON, tut. 14)
```

## 4. Graceful shutdown

Platforms stop apps by sending SIGTERM. Default behavior kills
connections mid-request; graceful shutdown stops ACCEPTING new
requests, finishes in-flight ones (up to a timeout), then exits:

```properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=20s
```

Verified — `kill <pid>` on the prod jar logged:

```
Commencing graceful shutdown. Waiting for active requests to complete
```

## 5. The Dockerfile

`Dockerfile` packages the whole build into an image (see the file —
it is commented line by line). The shape matters more than the
syntax:

- **Multi-stage**: stage 1 (JDK + wrapper) builds; stage 2 (JRE
  only) ships. Build tools never reach production.
- **Layer caching**: pom.xml is copied and dependencies resolved
  BEFORE the source — editing code doesn't re-download the world.
- **Non-root user**: a container breakout should not find root.
- Configuration still enters via `-e` env vars — same contract as
  section 3, which is exactly the point.

Honesty note: Docker is not installed on this machine, so
`docker build` itself was not run. Every step the Dockerfile
performs — `./mvnw clean package`, `java -jar` with prod env vars —
was verified directly above; the Dockerfile just runs the same
commands inside an image. When you install Docker Desktop:
`docker build -t bookshop .` then the `docker run` from the file
header.

## 6. The common mistakes

1. **Exposing all actuator endpoints publicly.** `/actuator/env`
   leaks every property (secrets redacted, but still a map of your
   system); heapdump leaks everything. Health public, rest locked —
   as configured.
2. **Panicking at a 503 health check during startup** — that is
   readiness working. It flips to UP when startup tasks finish.
3. **Building configuration INTO the image** (copying a filled-in
   prod properties file). The image should be environment-agnostic;
   config arrives at RUN time via env vars.
4. **`java -jar` works but Docker image dies instantly**: usually a
   missing env var — same fail-fast behavior as section 3, now one
   layer removed. Read the container logs; the answer is there.

## 7. Recap

- Actuator: public health for machines (readiness 503 during
  startup is correct), everything else authenticated.
- One jar, built by the wrapper, runs anywhere; env vars complete
  it; missing config kills it at startup by design.
- Graceful shutdown drains in-flight requests on SIGTERM.
- The Dockerfile is those same verified steps, containerized:
  multi-stage, cached deps, non-root.

Next: [**Tutorial 20 — The template**](tutorial-20%20%28The%20template%29.md): turning this project into the
starter you copy for every new backend, git history included.
