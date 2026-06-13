-- =============================================================================
-- Migración 04 — Etapa 1.4: dni nullable (para usuarios de Google OAuth)
-- =============================================================================
-- Las cuentas creadas vía Google no tienen DNI al momento del alta. Se relaja
-- la restricción NOT NULL (la unicidad se mantiene: Postgres permite varios NULL).
-- Los usuarios locales siguen obligados a cargar DNI (validado en el registro).
-- Reversible y sin pérdida de datos.
-- =============================================================================

ALTER TABLE usuario ALTER COLUMN dni DROP NOT NULL;
