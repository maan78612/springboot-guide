# TUTORIAL 19 — containerizing the app.
# Multi-stage: stage 1 has the JDK and Maven wrapper and builds the
# jar; stage 2 is a slim JRE-only image that ships. Build machinery
# never reaches production.
#
#   docker build -t bookshop .
#   docker run -p 8080:8080 \
#     -e SPRING_PROFILES_ACTIVE=prod \
#     -e JWT_SECRET=... -e DB_URL=... -e DB_USER=... -e DB_PASSWORD=... \
#     bookshop
#
# (Requires Docker. Verified logic-wise via the identical local
#  sequence: ./mvnw clean package + java -jar with the same env vars
#  - see the tutorial doc.)

# ── Stage 1: build ──────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
# Copy the wrapper + pom first: Docker caches this layer, so
# dependencies re-download only when pom.xml changes, not on every
# source edit.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -q dependency:go-offline
COPY src src
RUN ./mvnw -q clean package -DskipTests

# ── Stage 2: run ────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre
WORKDIR /app
# Never run as root inside the container.
RUN useradd --system --uid 1001 appuser
USER appuser
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
