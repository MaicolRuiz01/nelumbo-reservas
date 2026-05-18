# ============================================================
# Dockerfile - microservicio-notificaciones (Nelumbo)
# Multi-stage build: imagen final solo con JRE + jar.
# ============================================================

# -----------------------------
# STAGE 1: build con Maven
# -----------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src src
RUN mvn -B -q clean package -DskipTests \
    && cp target/*.jar /workspace/app.jar

# -----------------------------
# STAGE 2: runtime con JRE
# -----------------------------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /workspace/app.jar app.jar
RUN chown -R spring:spring /app
USER spring

EXPOSE 8081

# Healthcheck simple: el server responde HTTP (aunque sea 405 si pegamos GET a un POST).
# Si el TCP esta cerrado, wget falla y el contenedor se marca unhealthy.
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=10 \
    CMD wget -q -O- --server-response http://localhost:8081/notificaciones 2>&1 \
        | grep -qE "HTTP/1\.[01] (200|400|404|405)" || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
