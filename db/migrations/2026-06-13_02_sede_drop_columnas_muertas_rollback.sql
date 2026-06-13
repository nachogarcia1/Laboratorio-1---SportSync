-- =============================================================================
-- Rollback Migración 02 — sede
-- =============================================================================
-- Recrea las columnas latitud/longitud como NULL. Eran columnas muertas sin
-- datos (todas las filas estaban en NULL antes del DROP), por lo que no hay
-- información que recuperar: el rollback solo restituye la estructura.
-- =============================================================================

ALTER TABLE sede ADD COLUMN IF NOT EXISTS latitud  double precision;
ALTER TABLE sede ADD COLUMN IF NOT EXISTS longitud double precision;
