-- =============================================================================
-- Migración 06 — Etapa 3: extras de canchas (stock + sede + seed)
-- =============================================================================
-- Amplía item_equipamiento con stock e relación opcional a sede (sede_id NULL =
-- ítem global, disponible en todas las sedes). Siembra los extras de ejemplo
-- (guantes, pecheras, botines, pelota) como globales si no existen.
-- Idempotente. Reversible.
-- =============================================================================

ALTER TABLE item_equipamiento ADD COLUMN IF NOT EXISTS stock integer NOT NULL DEFAULT 0;
ALTER TABLE item_equipamiento ADD COLUMN IF NOT EXISTS sede_id bigint REFERENCES sede(id);

-- Seed de extras globales (solo si no existe uno con ese nombre)
INSERT INTO item_equipamiento (nombre, precio_por_unidad, stock, disponible, sede_id)
SELECT 'Guantes de arquero', 800, 10, true, NULL
WHERE NOT EXISTS (SELECT 1 FROM item_equipamiento WHERE nombre = 'Guantes de arquero');

INSERT INTO item_equipamiento (nombre, precio_por_unidad, stock, disponible, sede_id)
SELECT 'Pecheras', 300, 20, true, NULL
WHERE NOT EXISTS (SELECT 1 FROM item_equipamiento WHERE nombre = 'Pecheras');

INSERT INTO item_equipamiento (nombre, precio_por_unidad, stock, disponible, sede_id)
SELECT 'Botines', 600, 8, true, NULL
WHERE NOT EXISTS (SELECT 1 FROM item_equipamiento WHERE nombre = 'Botines');

INSERT INTO item_equipamiento (nombre, precio_por_unidad, stock, disponible, sede_id)
SELECT 'Pelota', 500, 15, true, NULL
WHERE NOT EXISTS (SELECT 1 FROM item_equipamiento WHERE nombre = 'Pelota');
