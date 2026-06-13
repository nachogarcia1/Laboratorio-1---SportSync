-- =============================================================================
-- Migración 02 — sede: eliminar columnas muertas latitud/longitud
-- =============================================================================
-- El proyecto conserva la implementación PostGIS: la ubicación de cada sede se
-- almacena en la columna `ubicacion geometry(Point,4326)`. Las columnas
-- `latitud`/`longitud` (double) son esquema muerto que dejó ddl-auto=update
-- durante el rollback (cuando la entidad usó columnas planas en vez del Point).
--
-- Ningún código backend ni frontend referencia las columnas físicas:
--   - Sede.java: latitud/longitud son @Transient + getters @JsonProperty
--     derivados de `ubicacion`.
--   - SedeRepository usa findByUbicacionIsNull().
--   - El frontend lee s.latitud/s.longitud del JSON (getters), no de la DB.
--
-- GUARDA DE SEGURIDAD: aborta si alguna sede tiene lat/lng cargados pero
-- ubicacion NULL (datos sin migrar). En ese caso NO se borra nada.
-- =============================================================================

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM sede
    WHERE (latitud IS NOT NULL OR longitud IS NOT NULL)
      AND ubicacion IS NULL
  ) THEN
    RAISE EXCEPTION 'Abortado: existen sedes con latitud/longitud NO migradas a ubicacion (PostGIS). Migrar esos datos antes de eliminar las columnas.';
  END IF;
END $$;

ALTER TABLE sede DROP COLUMN IF EXISTS latitud;
ALTER TABLE sede DROP COLUMN IF EXISTS longitud;
