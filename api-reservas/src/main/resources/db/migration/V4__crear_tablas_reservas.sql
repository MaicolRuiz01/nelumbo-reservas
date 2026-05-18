-- ============================================================
-- V4: Crear tablas de reservas y reservas históricas
-- ============================================================

-- ============================================================
-- Tabla: reservas
-- Almacena reservas activas, pendientes, rechazadas o expiradas
-- ============================================================

CREATE TABLE reservas (
                          id                      BIGSERIAL PRIMARY KEY,

                          documento_cliente       VARCHAR(12)  NOT NULL,
                          nombre_cliente          VARCHAR(150) NOT NULL,

                          fecha_inicio            TIMESTAMP    NOT NULL,
                          fecha_fin_estimada      TIMESTAMP    NOT NULL,
                          fecha_creacion          TIMESTAMP    NOT NULL,

                          asistentes              INTEGER      NOT NULL CHECK (asistentes > 0),

                          estado                  VARCHAR(30)  NOT NULL
                              CHECK (estado IN (
                                                'ACTIVA',
                                                'PENDIENTE_APROBACION',
                                                'RECHAZADA',
                                                'EXPIRADA'
                                  )),

                          motivo_rechazo          VARCHAR(500),

                          salon_id                BIGINT       NOT NULL REFERENCES salones(id),

                          gestor_id               BIGINT       NOT NULL REFERENCES usuarios(id)
);

-- ============================================================
-- Índices tabla reservas
-- ============================================================

CREATE INDEX idx_reservas_documento
    ON reservas(documento_cliente);

CREATE INDEX idx_reservas_estado
    ON reservas(estado);

CREATE INDEX idx_reservas_salon
    ON reservas(salon_id);

CREATE INDEX idx_reservas_fecha_inicio
    ON reservas(fecha_inicio);

CREATE INDEX idx_reservas_fecha_fin
    ON reservas(fecha_fin_estimada);

-- ============================================================
-- Tabla: reservas_historicas
-- Almacena reservas finalizadas
-- ============================================================

CREATE TABLE reservas_historicas (
                                     id                          BIGSERIAL PRIMARY KEY,

                                     documento_cliente           VARCHAR(12)  NOT NULL,
                                     nombre_cliente              VARCHAR(150) NOT NULL,

                                     fecha_inicio                TIMESTAMP    NOT NULL,
                                     fecha_fin_estimada          TIMESTAMP    NOT NULL,
                                     fecha_creacion              TIMESTAMP    NOT NULL,

                                     fecha_finalizacion_real     TIMESTAMP    NOT NULL,

                                     asistentes                  INTEGER      NOT NULL CHECK (asistentes > 0),

                                     total_cobrado               NUMERIC(15, 2) NOT NULL
                                         CHECK (total_cobrado >= 0),

                                     salon_id                    BIGINT       NOT NULL REFERENCES salones(id),

                                     gestor_id                   BIGINT       NOT NULL REFERENCES usuarios(id)
);



CREATE INDEX idx_reservas_historicas_documento
    ON reservas_historicas(documento_cliente);

CREATE INDEX idx_reservas_historicas_salon
    ON reservas_historicas(salon_id);

CREATE INDEX idx_reservas_historicas_gestor
    ON reservas_historicas(gestor_id);