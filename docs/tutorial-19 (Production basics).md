# Tutorial 19 — Production basics

Package the application for production deployment. Learn how to expose health checks with Spring Boot Actuator, build a self-contained executable JAR, configure applications via environment variables, enable graceful shutdown, and containerize with a multi-stage Dockerfile.

Files for this stage:

- Updated: `pom.xml` (`spring-boot-starter-actuator`)
- Updated: `src/main/java/com/example/bookshop/config/SecurityConfig.java` (permit `/actuator/health`)
- Updated: `src/main/resources/application-prod.properties` (graceful shutdown)
- New: `Dockerfile`
- New: `.dockerignore`

---

## 1. Add Health Checks with Spring Boot Actuator

Load balancers, reverse proxies, and Kubernetes orchestrators continuously poll health endpoints to detect healthy instances and route traffic away from failing servers.

Add `spring-boot-starter-actuator` to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Open `src/main/java/com/example/bookshop/config/SecurityConfig.java` and permit the health endpoint:
```java
.requestMatchers("/actuator/health").permitAll()
```

### Probing Health
```bash
curl -s http://localhost:8080/actuator/health
```
Response:
```json
{
  "status": "UP",
  "groups": ["liveness", "readiness"]
}
```

| Key / Endpoint | Why we use it |
| :--- | :--- |
| **Actuator** | Exposes production-ready operational endpoints (health, metrics, info). |
| `/actuator/health` | Lets deployment systems check whether the app is alive and ready to serve traffic. |
| **Readiness Check** | During startup tasks (e.g. database migrations), Actuator returns `503 OUT_OF_SERVICE`, preventing traffic from reaching half-started instances until initialization completes. |

> [!WARNING]
> Only expose `/actuator/health` publicly! Endpoints like `/actuator/env`, `/actuator/metrics`, and `/actuator/heapdump` can leak system configurations and memory dumps; they must remain locked behind authentication.

---

## 2. Build a Self-Contained Executable JAR

Spring Boot packages your compiled classes, static resources, database migrations, dependencies, and an embedded Apache Tomcat server into a single executable archive:

```bash
./mvnw clean package
```

Run the built JAR:
```bash
java -jar target/bookshop-0.0.1-SNAPSHOT.jar
```

The app becomes a single deployable artifact. There is no web server or container to install separately on your servers.

---

## 3. Configuration Through Environment Variables

Production deployments must never bake secret passwords or hardcoded URLs into the JAR file. The same immutable JAR file should run across development, staging, and production environments, configured entirely at runtime:

In `src/main/resources/application-prod.properties`:
```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
bookshop.security.jwt-secret=${JWT_SECRET}
```

Launch with production environment variables:
```bash
SPRING_PROFILES_ACTIVE=prod \
SERVER_PORT=8080 \
DB_URL=jdbc:postgresql://localhost:5432/bookshop \
DB_USER=bookshop \
DB_PASSWORD=secret \
JWT_SECRET=super-secret-jwt-signing-key-32-chars-long \
java -jar target/bookshop-0.0.1-SNAPSHOT.jar
```

Production config should be passed in at runtime, not hardcoded in the jar.

| Environment key | Purpose |
| :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Selects the production property profile (`application-prod.properties`). |
| `DB_URL` | Specifies the PostgreSQL connection URL. |
| `DB_USER` / `DB_PASSWORD` | Supplies database authentication credentials. |
| `JWT_SECRET` | Injects the secret signing key for token verification. |

> [!TIP]
> Notice that `${DB_URL}` has **no fallback value**. If an operator starts the production profile without providing the database connection variables, the application immediately fails fast with an exit code of 1, preventing degraded partial runs.

---

## 4. Graceful Shutdown

When deployment platforms (Docker, Kubernetes) restart an application, they send a `SIGTERM` signal. Without graceful shutdown, active HTTP requests in flight are abruptly dropped.

Enable graceful shutdown in `src/main/resources/application-prod.properties`:

```properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=20s
```

When a termination signal arrives:
1. Tomcat stops accepting new inbound connections.
2. Spring allows active requests up to 20 seconds to complete cleanly.
3. Database pools drain connections and the JVM exits safely.

---

## 5. Containerize with a Multi-Stage Dockerfile

Create `Dockerfile` in the root of the project:

```dockerfile
# Stage 1: Build the application using the full JDK
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw clean package -DskipTests

# Stage 2: Create a minimal, secure runtime image using JRE
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Key Production Docker Principles
- **Multi-Stage Build**: Compilers and Maven tooling stay in the build stage. The final production image only contains the lightweight JRE.
- **Layer Caching**: `pom.xml` and dependencies are fetched before copying source code, drastically speeding up rebuilds.
- **Non-Root Execution**: Runs under a dedicated unprivileged user (`appuser`), minimizing security vulnerabilities if the container is compromised.

Next: [**Tutorial 20 — The template**](tutorial-20%20%28The%20template%29.md)
