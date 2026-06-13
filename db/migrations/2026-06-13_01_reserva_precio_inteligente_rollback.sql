-- =============================================================================
-- Rollback Migración 01 — reserva
-- =============================================================================
-- Elimina las columnas de Precios Inteligentes. Las filas existentes conservan
-- su precio_total intacto (no se tocó). Seguro de ejecutar.
-- NOTA: con el código de precios mergeado activo, al reiniciar el backend con
-- ddl-auto=update Hibernate intentará recrearlas y volverá a fallar por las
-- filas existentes. Usar este rollback solo si también se revierte el código.
-- =============================================================================

ALTER TABLE reserva DROP COLUMN IF EXISTS descuento_aplicado;
ALTER TABLE reserva DROP COLUMN IF EXISTS precio_base;
