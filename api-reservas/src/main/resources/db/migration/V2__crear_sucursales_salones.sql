CREATE TABLE sucursales (
                            id         BIGSERIAL    PRIMARY KEY,
                            nombre     VARCHAR(100) NOT NULL,
                            ciudad     VARCHAR(100) NOT NULL,
                            direccion  VARCHAR(255) NOT NULL,
                            gestor_id  BIGINT       NOT NULL REFERENCES usuarios(id)
);

CREATE TABLE salones (
                         id             BIGSERIAL      PRIMARY KEY,
                         nombre         VARCHAR(100)   NOT NULL,
                         capacidad      INTEGER        NOT NULL CHECK (capacidad > 0),
                         costo_por_hora NUMERIC(15, 2) NOT NULL CHECK (costo_por_hora > 0),
                         sucursal_id    BIGINT         NOT NULL REFERENCES sucursales(id)
);