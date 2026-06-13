-- =============================================================================
-- Rollback Migración 03 — Etapa 1
-- =============================================================================
-- Elimina la tabla de códigos y las columnas de verificación de usuario.
-- Las cuentas conservan todos sus demás datos.
-- =============================================================================

DROP TABLE IF EXISTS codigo_verificacion;

ALTER TABLE usuario DROP COLUMN IF EXISTS auth_provider;
ALTER TABLE usuario DROP COLUMN IF EXISTS verificado;
