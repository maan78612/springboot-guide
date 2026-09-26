# Tutorial 19 — Production basics

Package the app for real deployment.

Files for this stage:

- Updated: `pom.xml`
- Updated: `src/main/java/com/example/bookshop/config/SecurityConfig.java`
- Updated: `src/main/resources/application-prod.properties`
- New: `Dockerfile`

---

## 1. Add health checks

```bash
GET /actuator/health
```

This gives infrastructure a lightweight readiness endpoint.

## 2. Build a runnable jar

```bash
./mvnw clean package
java -jar target/bookshop-0.0.1-SNAPSHOT.jar
```

The app becomes a single deployable artifact.

## 3. Use environment variables for config

```bash
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://localhost:5432/bookshop
DB_USER=bookshop
DB_PASSWORD=secret
```

Production config should be passed in at runtime, not hardcoded in the jar.

Next: [**Tutorial 20 — The template**](tutorial-20%20%28The%20template%29.md)
