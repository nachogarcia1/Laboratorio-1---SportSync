-- =============================================================================
-- Migración 09 — Sede: latitud/longitud como columnas simples (mapa robusto)
-- =============================================================================
-- El Point PostGIS se guardaba como objeto Java serializado (frágil, no sembrable
-- por SQL). Se agregan columnas latitud/longitud (double) como fuente de verdad
-- para el mapa, y se cargan coordenadas reales aproximadas de las sedes existentes.
-- =============================================================================

ALTER TABLE sede ADD COLUMN IF NOT EXISTS latitud  double precision;
ALTER TABLE sede ADD COLUMN IF NOT EXISTS longitud double precision;

UPDATE sede SET latitud = -34.5780, longitud = -58.4300 WHERE id = 6;  -- Palermo
UPDATE sede SET latitud = -34.5920, longitud = -58.3750 WHERE id = 7;  -- Retiro
UPDATE sede SET latitud = -34.4100, longitud = -58.6900 WHERE id = 8;  -- Benavidez
UPDATE sede SET latitud = -34.4585, longitud = -58.9140 WHERE id = 9;  -- Pilar
UPDATE sede SET latitud = -34.0980, longitud = -59.0290 WHERE id = 10; -- Zarate
