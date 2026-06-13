-- =============================================================================
-- Rollback Migración 04 — usuario.dni NOT NULL
-- =============================================================================
-- OJO: solo funciona si NO hay usuarios con dni NULL (p.ej. cuentas de Google).
-- Si los hay, primero hay que asignarles un DNI o eliminarlos.
-- =============================================================================

ALTER TABLE usuario ALTER COLUMN dni SET NOT NULL;
