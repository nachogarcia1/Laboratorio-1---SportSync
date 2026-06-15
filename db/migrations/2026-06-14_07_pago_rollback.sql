-- =============================================================================
-- Rollback Migración 07 — tabla pago
-- =============================================================================
-- Elimina la tabla de pagos. Las reservas en estado 'PENDIENTE_PAGO' deberían
-- normalizarse a otro estado antes de revertir, si las hubiera.
-- =============================================================================

DROP TABLE IF EXISTS pago;
