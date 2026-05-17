# Nelumbo — Sistema de Gestión de Reservas para Salones de Eventos

Solución a la prueba técnica de Backend de **Nelumbo Consultores**.

El proyecto está organizado como un **monorepo** con dos servicios independientes:

- **`api-reservas/`** — API REST principal. Spring Boot + PostgreSQL + JWT. Gestiona usuarios, sucursales, salones, reservas e indicadores.
- **`api-notificaciones/`** — Microservicio simulado de notificaciones por correo. (Se construye en la Fase 4.)

---

## Stack técnico

- Java 17
- Spring Boot 3.5.14
- Spring Security 6 + JJWT 0.12
- Spring Data JPA + Hibernate
- PostgreSQL 16
- Flyway (migraciones de BD)
- MapStruct (mapeo Entity ↔ DTO)
- Bean Validation (Hibernate Validator)
- Lombok
- springdoc-openapi (Swagger UI)
- JUnit 5 + Mockito
- Docker + Docker Compose

---

## Cómo levantar el proyecto en local

### Requisitos previos

- JDK 17
- Maven 3.9+ (o usar el wrapper `./mvnw` incluido)
- Docker Desktop (para levantar PostgreSQL)

### 1. Levantar la base de datos

Desde la raíz del repo:

```bash
docker compose up -d postgres
```

Esto inicia PostgreSQL en `localhost:5432` con:
- Base de datos: `nelumbo_reservas`
- Usuario: `postgres`
- Password: `postgres`

### 2. Ejecutar la API de reservas

```bash
cd api-reservas
./mvnw spring-boot:run
```

La API queda disponible en [http://localhost:8080](http://localhost:8080) y Swagger en [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).

### 3. Credenciales precargadas

```
email: admin@mail.com
pass:  admin
```

---

## Variables de entorno

| Variable | Default | Descripción |
|---|---|---|
| `DB_USER` | `postgres` | Usuario PostgreSQL |
| `DB_PASSWORD` | `postgres` | Password PostgreSQL |
| `JWT_SECRET` | (incluido en yaml) | Clave para firmar JWT (Base64, ≥ 256 bits) |
| `NOTIFICACIONES_URL` | `http://localhost:8081` | URL del microservicio de notificaciones |

---

## Estructura del repositorio

```
.
├── api-reservas/              # API principal
├── api-notificaciones/        # Microservicio simulado (Fase 4)
├── postman/                   # Colección Postman (Fase 6)
├── docs/                      # Diagrama ER y documentación adicional
├── docker-compose.yml         # Orquestación local
├── .gitignore
└── README.md
```

---

## Endpoints disponibles

### Autenticación (`/auth`)

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| POST | `/auth/login` | Inicio de sesión, devuelve JWT | Público |
| POST | `/auth/register` | Crea un usuario con rol GESTOR | Solo ADMIN |
| POST | `/auth/logout` | Invalida el token actual | Autenticado |

El token JWT tiene una duración de **6 horas** y se envía en cada petición protegida en el header:

```
Authorization: Bearer <token>
```

## Notas técnicas

- **Logout**: la invalidación de tokens se hace mediante una blacklist en memoria que se limpia automáticamente cada 30 minutos. Para producción se recomendaría usar Redis.
- **Passwords**: se almacenan hasheados con BCrypt.
- **Migraciones**: la estructura de la base de datos se gestiona con Flyway (`src/main/resources/db/migration`).

## Estado del desarrollo

- [x] Fase 0 — Setup del monorepo
- [x] Fase 1 — Autenticación y seguridad JWT
- [ ] Fase 2 — CRUD de Sucursales y Salones
- [ ] Fase 3 — Reservas (núcleo de negocio)
- [ ] Fase 4 — Microservicio de Notificaciones
- [ ] Fase 5 — Indicadores y métricas
- [ ] Fase 6 — Calidad y entrega final
