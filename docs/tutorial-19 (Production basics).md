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

| Key / Endpoint     | Why we use it                                       |
| ------------------ | --------------------------------------------------- |
| Actuator           | Exposes operational and health information          |
| `/actuator/health` | Lets deployment systems check whether the app is up |

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

| Environment key        | Purpose                                 |
| ---------------------- | --------------------------------------- |
| SPRING_PROFILES_ACTIVE | Selects the production property profile |
| DB_URL                 | Selects the PostgreSQL connection       |
| DB_USER / DB_PASSWORD  | Supplies database credentials           |

Next: [**Tutorial 20 — The template**](tutorial-20%20%28The%20template%29.md)
