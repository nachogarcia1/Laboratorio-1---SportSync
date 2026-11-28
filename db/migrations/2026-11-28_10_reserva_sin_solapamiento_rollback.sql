-- =============================================================================
-- Rollback Migración 10 — reserva: restricción anti-solapamiento
-- =============================================================================
-- Elimina la restricción. No se pierde ningún dato. Después de esto la base vuelve
-- a permitir reservas solapadas: dos pedidos simultáneos pueden reservar el mismo
-- turno. La extensión btree_gist se deja instalada (no molesta).
-- =============================================================================

ALTER TABLE reserva DROP CONSTRAINT IF EXISTS reserva_sin_solapamiento;
