-- Rollback Migración 08 — horario por cancha
ALTER TABLE cancha DROP COLUMN IF EXISTS dias_semana;
ALTER TABLE cancha DROP COLUMN IF EXISTS duracion_turno_min;
ALTER TABLE cancha DROP COLUMN IF EXISTS hora_cierre;
ALTER TABLE cancha DROP COLUMN IF EXISTS hora_apertura;
