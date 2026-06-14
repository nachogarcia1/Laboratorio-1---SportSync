-- =============================================================================
-- Rollback Migración 06 — equipamiento stock + sede
-- =============================================================================
-- Elimina las columnas agregadas. (No borra los ítems sembrados; si se desea,
-- eliminarlos a mano por nombre.)
-- =============================================================================

ALTER TABLE item_equipamiento DROP COLUMN IF EXISTS sede_id;
ALTER TABLE item_equipamiento DROP COLUMN IF EXISTS stock;
