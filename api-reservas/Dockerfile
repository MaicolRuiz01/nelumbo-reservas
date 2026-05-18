# ============================================================
# Dockerfile - api-reservas (Nelumbo)
# Multi-stage build: imagen final solo con JRE + jar.
# ============================================================

# -----------------------------
# STAGE 1: build con Maven
# Usamos la imagen oficial de maven con JDK 17 para evitar
# depender de mvnw (que en Alpine puede no encontrar unzip/tar).
# -----------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copiamos primero el pom para cachear la descarga de dependencias
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Copiamos el resto del codigo y empaquetamos (sin tests para acelerar la imagen)
COPY src src
RUN mvn -B -q clean package -DskipTests \
    && cp target/*.jar /workspace/app.jar

# -----------------------------
# STAGE 2: runtime con JRE
# -----------------------------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Usuario no-root por buenas practicas
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /workspace/app.jar app.jar
RUN chown -R spring:spring /app
USER spring

EXPOSE 8080

# Healthcheck contra Actuator. Si /actuator/health no responde, el contenedor se marca unhealthy.
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=10 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
