-- ============================================================
-- V1: Esquema inicial - Sistema de Reservas Nelumbo
-- ============================================================

-- Tabla: usuarios
-- Almacena ADMIN y GESTORES del sistema.
CREATE TABLE usuarios (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(150) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    nombre          VARCHAR(150) NOT NULL,
    rol             VARCHAR(20)  NOT NULL CHECK (rol IN ('ADMIN', 'GESTOR')),
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_usuarios_email ON usuarios (email);
CREATE INDEX idx_usuarios_rol   ON usuarios (rol);

-- NOTA: El seed del usuario administrador (admin@mail.com / admin)
-- se realiza desde Java mediante un CommandLineRunner en la Fase 1,
-- para que el hash BCrypt sea generado por el mismo PasswordEncoder
-- que se usa para autenticar.
