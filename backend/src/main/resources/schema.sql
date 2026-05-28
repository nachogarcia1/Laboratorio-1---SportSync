-- Habilita la extensión PostGIS en la base de datos.
-- Se ejecuta en cada arranque antes de que Hibernate corra el DDL (defer-datasource-initialization=false).
-- IF NOT EXISTS lo hace idempotente: no falla si ya está habilitada.
CREATE EXTENSION IF NOT EXISTS postgis;
