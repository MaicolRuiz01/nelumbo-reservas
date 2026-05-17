-- ============================================================
-- V3: Agrega gestor responsable a cada salon
--
-- Antes los salones heredaban el gestor de su sucursal.
-- Ahora cada salon tiene su propio gestor, alineado con el PDF.
-- ============================================================

-- 1) Agregamos la columna permitiendo NULL temporalmente
ALTER TABLE salones
    ADD COLUMN gestor_id BIGINT REFERENCES usuarios(id);

-- 2) Inicializamos cada salon con el gestor de su sucursal
--    para no romper los datos que ya existen.
UPDATE salones s
SET gestor_id = (
    SELECT su.gestor_id
    FROM sucursales su
    WHERE su.id = s.sucursal_id
);

-- 3) Una vez poblada, la columna queda como NOT NULL
ALTER TABLE salones
    ALTER COLUMN gestor_id SET NOT NULL;
