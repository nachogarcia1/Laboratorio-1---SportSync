-- =============================================================================
-- Migración 10 — reserva: restricción anti-solapamiento (control de concurrencia)
-- =============================================================================
-- Problema: ReservaService verificaba el solapamiento con una consulta y después
-- guardaba, sin ningún bloqueo entre ambos pasos. Dos pedidos simultáneos para el
-- mismo turno pasaban la verificación y creaban dos reservas.
--
-- Solución: una restricción de exclusión de PostgreSQL. La base de datos garantiza
-- que no puede haber dos reservas ACTIVA o PENDIENTE_PAGO que se superpongan en el
-- tiempo para la misma cancha. Si dos pedidos compiten, uno falla con SQLSTATE
-- 23P01 (exclusion_violation) y el backend lo traduce a HTTP 409.
--
-- Detalles:
--   * El rango es [inicio, fin): un turno 18:00-19:00 y otro 19:00-20:00 NO chocan.
--   * Las reservas CANCELADA y RECHAZADA no cuentan: cancelar libera el turno.
--   * Si hora_fin <= hora_inicio se interpreta que el turno termina al día
--     siguiente (por ejemplo 23:00-00:00).
--   * Requiere la extensión btree_gist (viene con PostgreSQL; en servicios
--     administrados como Neon o Supabase se habilita con CREATE EXTENSION).
--
-- GUARDA DE SEGURIDAD: si ya hay reservas ACTIVA/PENDIENTE_PAGO solapadas en la
-- base, la migración ABORTA sin cambiar nada (la restricción no se podría crear).
-- Para encontrarlas:
--   SELECT a.id AS reserva_a, b.id AS reserva_b, a.cancha_id, a.fecha,
--          a.hora_inicio, a.hora_fin, b.hora_inicio, b.hora_fin
--   FROM reserva a JOIN reserva b ON a.id < b.id AND a.cancha_id = b.cancha_id
--     AND a.fecha = b.fecha AND a.hora_inicio < b.hora_fin AND a.hora_fin > b.hora_inicio
--   WHERE a.estado IN ('ACTIVA','PENDIENTE_PAGO') AND b.estado IN ('ACTIVA','PENDIENTE_PAGO');
-- y cancelá (estado = 'CANCELADA') las que sobren antes de volver a aplicar.
--
-- Idempotente. Reversible (ver el _rollback.sql).
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS btree_gist;

DO $$
DECLARE
  cantidad integer;
BEGIN
  SELECT count(*) INTO cantidad
  FROM reserva a
  JOIN reserva b
    ON a.id < b.id
   AND a.cancha_id = b.cancha_id
   AND tsrange(a.fecha + a.hora_inicio,
               CASE WHEN a.hora_fin > a.hora_inicio THEN a.fecha + a.hora_fin ELSE a.fecha + 1 + a.hora_fin END,
               '[)')
    && tsrange(b.fecha + b.hora_inicio,
               CASE WHEN b.hora_fin > b.hora_inicio THEN b.fecha + b.hora_fin ELSE b.fecha + 1 + b.hora_fin END,
               '[)')
  WHERE a.estado IN ('ACTIVA', 'PENDIENTE_PAGO')
    AND b.estado IN ('ACTIVA', 'PENDIENTE_PAGO');

  IF cantidad > 0 THEN
    RAISE EXCEPTION 'Abortado: hay % par(es) de reservas ACTIVA/PENDIENTE_PAGO solapadas en la misma cancha. Cancelá las duplicadas (ver la consulta en el encabezado de esta migración) y volvé a aplicarla.', cantidad;
  END IF;
END $$;

ALTER TABLE reserva DROP CONSTRAINT IF EXISTS reserva_sin_solapamiento;

ALTER TABLE reserva ADD CONSTRAINT reserva_sin_solapamiento
  EXCLUDE USING gist (
    cancha_id WITH =,
    tsrange(fecha + hora_inicio,
            CASE WHEN hora_fin > hora_inicio THEN fecha + hora_fin ELSE fecha + 1 + hora_fin END,
            '[)') WITH &&
  ) WHERE (estado IN ('ACTIVA', 'PENDIENTE_PAGO'));
