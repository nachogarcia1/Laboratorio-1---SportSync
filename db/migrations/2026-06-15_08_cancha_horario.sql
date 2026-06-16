-- =============================================================================
-- Migración 08 — Horario por cancha (independiente del de la sede)
-- =============================================================================
-- Cada cancha pasa a tener su propio horario (apertura, cierre, duración de turno,
-- días de la semana). Las canchas EXISTENTES heredan el horario de su sede para
-- no perder disponibilidad (backfill). Reservas existentes no se ven afectadas.
-- =============================================================================

ALTER TABLE cancha ADD COLUMN IF NOT EXISTS hora_apertura       varchar(5)  NOT NULL DEFAULT '08:00';
ALTER TABLE cancha ADD COLUMN IF NOT EXISTS hora_cierre         varchar(5)  NOT NULL DEFAULT '22:00';
ALTER TABLE cancha ADD COLUMN IF NOT EXISTS duracion_turno_min  integer     NOT NULL DEFAULT 60;
ALTER TABLE cancha ADD COLUMN IF NOT EXISTS dias_semana         varchar(20) NOT NULL DEFAULT '1,2,3,4,5,6,7';

-- Backfill: las canchas existentes toman el horario de su sede
UPDATE cancha c
SET hora_apertura = s.hora_apertura,
    hora_cierre   = s.hora_cierre
FROM sede s
WHERE c.sede_id = s.id;
