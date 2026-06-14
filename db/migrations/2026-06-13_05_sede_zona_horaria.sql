-- =============================================================================
-- Migración 05 — Etapa 2: sede.zona_horaria
-- =============================================================================
-- Agrega la zona horaria IANA de cada sede, usada para determinar qué turnos ya
-- pasaron (validación de horarios vencidos). Se agrega NOT NULL con DEFAULT, así
-- las sedes existentes quedan en horario de Argentina sin romper.
-- Reversible y sin pérdida de datos.
-- =============================================================================

ALTER TABLE sede
    ADD COLUMN IF NOT EXISTS zona_horaria varchar(64) NOT NULL
    DEFAULT 'America/Argentina/Buenos_Aires';
