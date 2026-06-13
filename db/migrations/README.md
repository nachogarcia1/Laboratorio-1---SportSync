# Migraciones SQL manuales

El proyecto **no usa Flyway/Liquibase**. Estas migraciones son scripts SQL
reproducibles que se aplican a mano y quedan versionados. **No dependen de
`spring.jpa.hibernate.ddl-auto=update`** para los cambios que requieren
preservar datos o eliminar esquema.

Orden de aplicación:

| # | Archivo | Qué hace | Rollback |
|---|---------|----------|----------|
| 01 | `2026-06-13_01_reserva_precio_inteligente.sql` | Agrega `precio_base` y `descuento_aplicado` a `reserva` (estrategia NULL → backfill → NOT NULL) preservando las filas existentes | `..._rollback.sql` |
| 02 | `2026-06-13_02_sede_drop_columnas_muertas.sql` | Elimina las columnas muertas `latitud`/`longitud` de `sede` (el proyecto usa PostGIS `ubicacion`) | `..._rollback.sql` |

## Cómo aplicar

Con la base de datos levantada (`docker compose up -d db`):

```bash
# Migración 01 (reserva)
docker exec -i postgres_app psql -U postgres -d sport_db \
  < db/migrations/2026-06-13_01_reserva_precio_inteligente.sql

# Migración 02 (sede)
docker exec -i postgres_app psql -U postgres -d sport_db \
  < db/migrations/2026-06-13_02_sede_drop_columnas_muertas.sql
```

Para revertir, aplicar el `_rollback.sql` correspondiente.
