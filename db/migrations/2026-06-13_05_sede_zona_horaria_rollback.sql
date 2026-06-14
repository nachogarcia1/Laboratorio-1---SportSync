-- =============================================================================
-- Rollback Migración 05 — sede.zona_horaria
-- =============================================================================
ALTER TABLE sede DROP COLUMN IF EXISTS zona_horaria;
