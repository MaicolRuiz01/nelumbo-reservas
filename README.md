# Nelumbo — Sistema de Gestión de Reservas para Salones de Eventos

Solución a la prueba técnica de Backend de **Nelumbo Consultores**.

El proyecto está organizado como un **monorepo** con dos servicios independientes:

- **`api-reservas/`** — API REST principal. Spring Boot + PostgreSQL + JWT. Gestiona usuarios, sucursales, salones, reservas, notificaciones e indicadores.
- **`microservicio-notificaciones/`** — Microservicio simulado de notificaciones por correo. Recibe la solicitud, la registra en log y responde `{"mensaje":"Notificación Enviada"}`.

Ambos servicios se orquestan junto con PostgreSQL mediante `docker-compose.yml`.

---

## Stack técnico

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.5.14 |
| Seguridad | Spring Security 6 + JJWT 0.12 |
| Persistencia | Spring Data JPA + Hibernate + PostgreSQL 16 |
| Migraciones | Flyway (V1–V4) |
| Mapeo DTO↔Entity | MapStruct 1.6 |
| Validación | Bean Validation (Hibernate Validator) |
| HTTP Client | Spring WebFlux (WebClient) |
| Documentación | springdoc-openapi (Swagger UI) |
| Tests | JUnit 5 + Mockito |
| Build/Run | Maven + Docker + Docker Compose |
| Boilerplate | Lombok |

---

## Modelo Entidad-Relación

![MER](docs/mer.svg)

> El diagrama también está disponible como [fuente Mermaid](docs/mer.mermaid) y como [SVG](docs/mer.svg).

**Resumen de tablas:**

- `usuarios` — Cuentas del sistema (ADMIN y GESTOR).
- `sucursales` — Sucursales del negocio. Cada una tiene un gestor responsable.
- `salones` — Salones de eventos. Pertenecen a una sucursal y tienen un gestor responsable (puede ser distinto al de la sucursal).
- `reservas` — Reservas en curso: `ACTIVA`, `PENDIENTE_APROBACION`, `RECHAZADA` o `EXPIRADA`.
- `reservas_historicas` — Reservas finalizadas (check-out), con `fecha_finalizacion_real` y `total_cobrado`.

---

## Cómo levantar el proyecto

### Opción A — Docker (recomendada)

Levanta los **tres** servicios (Postgres + microservicio + API) con un solo comando.

**Requisitos:**
- Docker Desktop con `docker compose` (v2+).

```bash
docker compose up -d --build
```

Verifica con `docker compose ps` que los tres contenedores estén saludables:

```
nelumbo-postgres                       healthy
nelumbo-microservicio-notificaciones   healthy
nelumbo-api-reservas                   healthy
```

Servicios expuestos:
- API principal → http://localhost:8080
- Swagger UI → http://localhost:8080/swagger-ui.html
- Microservicio → http://localhost:8081
- PostgreSQL → `localhost:5432`

Para detener todo:

```bash
docker compose down            # conserva la base de datos
docker compose down -v         # borra también el volumen de Postgres
```

### Opción B — Ejecución local

Útil durante el desarrollo.

**Requisitos:**
- JDK 17
- Maven 3.9+ (puedes usar el wrapper `./mvnw` incluido)
- Docker Desktop (solo para PostgreSQL)

```bash
# 1) Solo la base de datos en Docker
docker compose up -d postgres

# 2) Microservicio de notificaciones (terminal 1)
cd microservicio-notificaciones
./mvnw spring-boot:run

# 3) API principal (terminal 2)
cd api-reservas
./mvnw spring-boot:run
```

> Por defecto la API arranca con perfil `dev` y se conecta a `localhost:5432`.
> Para forzar otro perfil: `./mvnw spring-boot:run -Dspring-boot.run.profiles=docker`.

### Credenciales precargadas

Al arrancar la API por primera vez se inserta un administrador semilla mediante `DataInitializer`:

```
email:    admin@mail.com
password: admin
```

---

## Variables de entorno

| Variable | Default | Descripción |
|---|---|---|
| `DB_USER` | `postgres` | Usuario PostgreSQL |
| `DB_PASSWORD` | `postgres` | Password PostgreSQL |
| `JWT_SECRET` | (valor incluido en `application.yaml`) | Clave Base64 (≥ 256 bits) para firmar JWT |
| `NOTIFICACIONES_URL` | `http://localhost:8081` (local) / `http://microservicio-notificaciones:8081` (docker) | URL del microservicio |
| `SPRING_PROFILES_ACTIVE` | `dev` (local) / `docker` (compose) | Perfil de Spring |

---

## Estructura del repositorio

```
.
├── api-reservas/                        # API principal (Spring Boot, puerto 8080)
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── microservicio-notificaciones/        # Micro simulado (Spring Boot, puerto 8081)
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── postman/                             # Colección y environment Postman
│   ├── Reservas.postman_collection.json
│   └── Local.postman_environment.json
├── docs/                                # Diagramas y documentación
│   ├── mer.mermaid
│   └── mer.svg
├── docker-compose.yml                   # Orquestación local
├── .gitignore
└── README.md
```

---

## Autenticación

El token JWT tiene una duración de **6 horas**. Se envía en el header en cada petición protegida:

```
Authorization: Bearer <token>
```

El **logout** invalida el token añadiéndolo a una blacklist en memoria (un `ConcurrentHashMap` con limpieza programada cada 30 min vía `@Scheduled`). En producción se recomendaría reemplazarlo por Redis o un store distribuido.

---

## Endpoints

> Todas las rutas devuelven los errores con el formato unificado:
> ```json
> { "mensaje": "..." }
> ```
> emitido por `GlobalExceptionHandler` (`@ControllerAdvice`).

### Autenticación (`/auth`)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/auth/login` | Público | Login. Devuelve `{ token, email, rol, expiresAt }`. |
| POST | `/auth/register` | ADMIN | Crea un usuario con rol GESTOR. |
| POST | `/auth/logout` | Autenticado | Invalida el token actual. |

### Sucursales (`/sucursales`)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/sucursales` | ADMIN | Crea sucursal (requiere `gestorId`). |
| GET | `/sucursales` | ADMIN · GESTOR | ADMIN ve todas; GESTOR solo las suyas. |
| GET | `/sucursales/{id}` | ADMIN · GESTOR | Detalle (con ownership para GESTOR). |
| PUT | `/sucursales/{id}` | ADMIN | Actualización. |
| DELETE | `/sucursales/{id}` | ADMIN | Falla si la sucursal tiene salones. |

### Salones (`/salones`)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/salones` | ADMIN | Crea salón. |
| GET | `/salones` | ADMIN · GESTOR | ADMIN ve todos; GESTOR solo los de sus sucursales. |
| GET | `/salones/{id}` | ADMIN · GESTOR | Detalle (con ownership para GESTOR). |
| PUT | `/salones/{id}` | ADMIN | Actualización. |
| DELETE | `/salones/{id}` | ADMIN | Eliminación. |

### Reservas (`/reservas`)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/reservas` | ADMIN · GESTOR | Registrar reserva. Marca como `PENDIENTE_APROBACION` si el costo estimado supera 500.000. |
| POST | `/reservas/finalizar` | ADMIN · GESTOR | Check-out. Mueve la reserva a histórico, calcula `totalCobrado`. |
| GET | `/reservas/salon/{salonId}` | ADMIN · GESTOR | Lista activas en un salón (con ownership). |
| GET | `/reservas/buscar?documento=` | ADMIN · GESTOR | Búsqueda parcial por documento (LIKE `%xxx%`). |
| POST | `/reservas/{id}/aprobar` | ADMIN | Aprueba una `PENDIENTE_APROBACION` → `ACTIVA`. Notifica al gestor. |
| POST | `/reservas/{id}/rechazar` | ADMIN | Rechaza una `PENDIENTE_APROBACION` → `RECHAZADA` (motivo obligatorio). |

#### POST `/reservas` — Body

```json
{
  "documentoCliente": "1234567890",
  "nombreCliente": "Juan Pérez",
  "fechaInicio": "2026-06-01T18:00:00",
  "fechaFinEstimada": "2026-06-01T22:00:00",
  "asistentes": 50,
  "salonId": 1
}
```

**Respuesta 201:** `{ "id": 42 }`

**Errores típicos (HTTP 400):**

| Caso | `mensaje` |
|---|---|
| Documento ya tiene reserva activa | `No se puede Registrar Reserva, ya existe una reserva activa para este documento en este u otro salón` |
| Capacidad excedida en el horario | `No se puede Registrar Reserva, capacidad insuficiente en el salón` |
| Fechas inválidas | `La fecha de fin estimada debe ser posterior a la fecha de inicio` |

#### POST `/reservas/finalizar` — Body

```json
{ "documentoCliente": "1234567890", "salonId": 1 }
```

**Respuesta 200:**

```json
{ "mensaje": "Reserva finalizada", "totalCobrado": 600000 }
```

**Error (400):** `No se puede Finalizar Reserva, no existe una reserva activa para este documento en el salón`

### Notificaciones (`/notificaciones`)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/notificaciones` | ADMIN · GESTOR | Envía notificación al gestor para una reserva activa. |

**Body:**

```json
{
  "email": "gestor@mail.com",
  "documento": "1234567890",
  "mensaje": "Recordatorio: tu reserva es hoy",
  "salonId": 1
}
```

La API valida que exista una reserva **ACTIVA** para ese `documento + salonId`, **transforma `salonId` en `salonNombre`** y delega al microservicio. Si no existe, error 400:

```
No se puede Enviar Notificación, no existe una reserva activa para este documento en el salón indicado
```

### Indicadores (`/indicadores`)

| Método | Ruta | Acceso | Indicador del PDF |
|---|---|---|---|
| GET | `/indicadores/top-clientes` | ADMIN · GESTOR | Top 10 clientes con más reservas en el sistema (1). |
| GET | `/indicadores/top-clientes/salon/{salonId}` | ADMIN · GESTOR | Top 10 clientes por salón (2). |
| GET | `/indicadores/clientes-primera-vez/salon/{salonId}` | ADMIN · GESTOR | Reservas activas de clientes que reservan por primera vez en ese salón (3). |
| GET | `/indicadores/ganancias/salon/{salonId}` | GESTOR (sus salones) · ADMIN | Ganancias hoy / semana / mes / año (4). |
| GET | `/indicadores/top-sucursales` | ADMIN | Top 3 sucursales con mayor facturación del mes actual (5). |
| GET | `/reservas/buscar?documento=` | ADMIN · GESTOR | Búsqueda parcial por documento (6). |

> El GESTOR solo accede a indicadores de **sus salones**. El ADMIN ve todo.

---

## Reglas de negocio críticas

Reglas tomadas literalmente del PDF de la prueba:

- **Documento del cliente:** entre 6 y 12 dígitos, únicamente numéricos.
- **Asistentes:** entero positivo, nunca cero.
- **Fechas:** `fechaFinEstimada` **estrictamente posterior** a `fechaInicio`.
- **Capacidad:** se valida contra la suma de asistentes de reservas que se solapan en el rango horario. Las reservas en estado `PENDIENTE_APROBACION` **no consumen cupo**.
- **Doble reserva:** un mismo documento no puede tener dos reservas activas, **ni siquiera en salones distintos**.
- **Cobro:** las horas se redondean **hacia arriba** (cualquier fracción menor a 1h se cobra como 1h completa).

### Reservas Premium

Cuando el costo estimado de una reserva (`horas × costoPorHora`) supera **500.000**:

1. Se registra en estado **`PENDIENTE_APROBACION`** y **no consume cupo** del salón.
2. Solo un **ADMIN** puede aprobarla (`POST /reservas/{id}/aprobar`) o rechazarla (`POST /reservas/{id}/rechazar` con motivo obligatorio).
3. Al aprobarse pasa a **`ACTIVA`** y se dispara una notificación al gestor responsable vía el microservicio. Si el microservicio está caído, la aprobación **no se revierte** (el side-effect está aislado en `try/catch` y se loguea WARN).
4. Pasadas **48 horas** sin aprobación, un job `@Scheduled` (`ReservaExpirationJob`) la marca como **`EXPIRADA`**.
5. `RECHAZADA` y `EXPIRADA` son estados terminales — el cliente debe generar una nueva reserva.

---

## Postman

La carpeta `postman/` contiene:

- `Reservas.postman_collection.json` — Colección completa con todos los endpoints organizados por carpeta (`1. Auth`, `2. Sucursales`, `3. Salones`, `4. Reservas`, `5. Notificaciones`, `6. Indicadores`).
- `Local.postman_environment.json` — Environment `Nelumbo Local` con las variables (`baseUrl`, `token`, etc.).

**Cómo usarla:**

1. En Postman, importa ambos archivos (botón *Import*).
2. Selecciona el environment **Nelumbo Local**.
3. Ejecuta `1. Auth → Login admin`. El test asociado guarda automáticamente el token en la variable `{{token}}`.
4. El resto de requests usan ese token vía `Authorization: Bearer {{token}}`.

---

## Documentación interactiva (Swagger)

Con la API arriba, Swagger UI está disponible en:

http://localhost:8080/swagger-ui.html

Y la especificación OpenAPI JSON en:

http://localhost:8080/v3/api-docs

---

## Notas técnicas

- **Logout / blacklist de tokens:** la invalidación se hace en memoria (`TokenBlacklistService`). Suficiente para esta prueba; en producción se reemplazaría por Redis para soportar múltiples instancias.
- **Passwords:** se almacenan hasheados con BCrypt.
- **Migraciones:** Flyway gestiona el esquema (`src/main/resources/db/migration`). Las migraciones ya aplicadas son inmutables — cualquier cambio implica una nueva versión `V5`, `V6`, …
- **Roles:** `ADMIN` tiene acceso completo; `GESTOR` solo opera sobre las sucursales y salones que tiene asociados (ownership validado en cada service).
- **Gestor por salón:** cada sucursal tiene un gestor responsable y cada salón también. Pueden ser el mismo gestor o gestores distintos.
- **Comunicación con el microservicio:** se usa `WebClient` (WebFlux) con timeouts configurables (`app.notificaciones.timeout-ms`). Si el microservicio falla en una notificación post-aprobación, **la reserva no se revierte** — el side-effect está aislado.
- **Excepciones personalizadas:** todos los errores de negocio usan excepciones del paquete `exception/` y se traducen a `400/403/404/502` en `GlobalExceptionHandler`. Nunca se lanza `RuntimeException` genérica desde un service.
- **Anti-corruption layer:** el cliente del microservicio tiene sus propios DTOs (`client/dto/*`) — el resto del dominio no conoce el contrato del micro.

---

## Estado del desarrollo

- [x] Fase 0 — Setup del monorepo
- [x] Fase 1 — Autenticación y seguridad JWT
- [x] Fase 2 — CRUD de Sucursales y Salones
- [x] Fase 3 — Reservas (núcleo de negocio)
- [x] Fase 4 — Microservicio de Notificaciones
- [x] Fase 5 — Indicadores y métricas
- [x] Fase 6 — Calidad y entrega final (Docker, README, MER)

---

## Autor

**Maicol Ruiz** · Prueba técnica para Nelumbo Consultores · 2026
