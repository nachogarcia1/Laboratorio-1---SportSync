-- =============================================================================
-- Migración 01 — reserva: precio_base + descuento_aplicado (Precios Inteligentes)
-- =============================================================================
-- Contexto: la entidad Reserva (feature/precios-inteligentes) agrega dos campos
-- NOT NULL. Agregarlos directamente como NOT NULL falla porque la tabla ya tiene
-- filas. Estrategia segura en 4 pasos, preservando los datos existentes:
--   1) ADD COLUMN permitiendo NULL
--   2) Backfill: precio_base = precio_total, descuento_aplicado = 0
--   3) Verificar (SELECT de control)
--   4) SET NOT NULL
-- Idempotente (IF NOT EXISTS / WHERE ... IS NULL).
-- =============================================================================

-- Paso 1 — Agregar columnas permitiendo NULL (no rompe filas existentes)
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS precio_base        double precision;
ALTER TABLE reserva ADD COLUMN IF NOT EXISTS descuento_aplicado double precision;

-- Paso 2 — Backfill de las filas existentes
UPDATE reserva SET precio_base        = precio_total WHERE precio_base IS NULL;
UPDATE reserva SET descuento_aplicado = 0            WHERE descuento_aplicado IS NULL;

-- Paso 3 — Verificación (no debe devolver filas con NULL)
-- SELECT id, precio_total, precio_base, descuento_aplicado FROM reserva ORDER BY id;
-- SELECT count(*) AS pendientes FROM reserva WHERE precio_base IS NULL OR descuento_aplicado IS NULL;

-- Paso 4 — Establecer NOT NULL (coincide con la entidad Reserva)
ALTER TABLE reserva ALTER COLUMN precio_base        SET NOT NULL;
ALTER TABLE reserva ALTER COLUMN descuento_aplicado SET NOT NULL;
