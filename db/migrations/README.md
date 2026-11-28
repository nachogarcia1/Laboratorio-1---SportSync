# Migraciones SQL manuales

El proyecto **no usa Flyway/Liquibase**. Estas migraciones son scripts SQL
reproducibles que se aplican a mano y quedan versionados. No hay una tabla que
registre cuáles se aplicaron: por eso todas están escritas para ser **idempotentes**
(se pueden volver a ejecutar sin romper nada), con la excepción marcada de la 02.

## Qué hace Hibernate y qué hacen los scripts

La app corre con `spring.jpa.hibernate.ddl-auto=update`. Al arrancar, Hibernate **crea las
tablas y columnas que faltan**, pero **no** borra ni renombra nada, **no** cambia constraints
que ya existen (por ejemplo `reserva_estado_check` cuando se agrega un valor a un enum), **no**
hace backfill de datos y **no** puede agregar una columna `NOT NULL` a una tabla que ya tiene filas.

| Cambio | ¿Alcanza con tocar la entidad? | ¿Hace falta script? |
|---|---|---|
| Tabla nueva o columna nullable nueva | Sí, para una base nueva | Sí, para las bases que ya tienen datos |
| Columna `NOT NULL` sobre tabla con filas | No (falla) | **Sí, obligatorio** (ADD NULL → backfill → SET NOT NULL) |
| Borrar o renombrar columna/tabla, cambiar tipo | No (Hibernate no lo hace) | **Sí, obligatorio** |
| Cambiar un constraint (CHECK, UNIQUE, FK) | No | **Sí, obligatorio** |
| Cargar o corregir datos | No | **Sí, obligatorio** |

Los scripts obligatorios se aplican **antes** de arrancar la versión nueva del backend.

Hoy `ddl-auto` es `update` en todos los entornos. Para un servidor compartido o producción lo
recomendable es `validate` (el backend revisa que el esquema coincida y no lo toca), pero no está
configurado: antes habría que contar con un esquema base conocido, que hoy no existe como script.

## Levantar una base vacía con el esquema completo

Las migraciones 01–09 **no** crean las tablas base (asumen que `usuario`, `sede`, `reserva`, etc. ya
existen), así que no sirven para construir una base desde cero. El esquema completo lo crea el backend:

```bash
docker compose up -d db                 # Postgres + PostGIS vacío (requiere POSTGRES_PASSWORD en el .env de la raíz)
cd backend && ./mvnw spring-boot:run    # primer arranque: schema.sql activa PostGIS y Hibernate crea las tablas
```

Para verificar que quedó completo:

```bash
docker exec postgres_app psql -U postgres -d sport_db -tAc \
  "select count(*) from information_schema.tables where table_schema='public'"
# esperado: 17  (14 tablas de la app + 3 de PostGIS)
```

Las 14 tablas de la app: `cancha`, `codigo_verificacion`, `critica_cancha`, `critica_usuario`,
`descuento_horario`, `item_equipamiento`, `membresia`, `mensaje_chat`, `pago`, `reserva`,
`reserva_equipamiento`, `sede`, `suspension`, `usuario`.

Una base creada así **ya tiene el esquema final**: no necesita aplicar las migraciones. Lo único que
esas migraciones agregan en datos es la 06 (4 extras globales de equipamiento: guantes de arquero,
pecheras, botines y pelota). Si querés esos extras en una base nueva, aplicá solo la 06 (es idempotente).

> **Probado el 2026-11-28** con una base descartable: una base vacía, tras el primer arranque del
> backend, tenía 17 tablas, PostGIS activo y las columnas `latitud`/`longitud`/`ubicacion` en `sede`.
> Después se aplicaron las 9 migraciones en orden, sin errores, y el backend volvió a arrancar sin
> errores de esquema.

### Resetear la base (borra todos los datos)

Con el **backend apagado**:

```bash
docker compose down -v      # baja los contenedores y borra el volumen postgres_data
docker compose up -d db     # base vacía
```

Después reiniciá el backend para que recree las tablas. Si reseteás la base con el backend
encendido, Hibernate **no** vuelve a crear las tablas (solo lo hace al arrancar) y todo falla con
`relation "usuario" does not exist`.

## Orden de las migraciones

| # | Archivo | Qué hace | Rollback |
|---|---------|----------|----------|
| 01 | `2026-06-13_01_reserva_precio_inteligente.sql` | Agrega `precio_base` y `descuento_aplicado` a `reserva` (NULL → backfill → NOT NULL), preservando las filas existentes | Borra las dos columnas. Solo tiene sentido si también se revierte el código |
| 02 | `2026-06-13_02_sede_drop_columnas_muertas.sql` | **OBSOLETA, no aplicar.** Eliminaba `latitud`/`longitud` de `sede`. La 09 las volvió a agregar y hoy `Sede.java` las usa | Recrea las columnas vacías |
| 03 | `2026-06-13_03_usuario_verificacion.sql` | Agrega `verificado` y `auth_provider` a `usuario` (los existentes quedan sin verificar) y crea `codigo_verificacion` | Borra la tabla y las columnas: **se pierden los códigos y el estado de verificación** |
| 04 | `2026-06-13_04_usuario_dni_nullable.sql` | `usuario.dni` pasa a admitir NULL (cuentas de Google) | Falla si hay usuarios con `dni` NULL |
| 05 | `2026-06-13_05_sede_zona_horaria.sql` | Agrega `sede.zona_horaria` (por defecto `America/Argentina/Buenos_Aires`) | Borra la columna |
| 06 | `2026-06-13_06_equipamiento_stock_sede.sql` | Agrega `stock` y `sede_id` a `item_equipamiento` y siembra 4 extras globales | Borra las columnas; **no** borra los ítems sembrados |
| 07 | `2026-06-14_07_pago.sql` | Crea la tabla `pago` y recrea `reserva_estado_check` incluyendo `PENDIENTE_PAGO` | Borra `pago` (**se pierden los pagos**) y **no** restaura el check anterior |
| 08 | `2026-06-15_08_cancha_horario.sql` | Agrega horario propio a `cancha` (apertura, cierre, duración de turno, días) y lo completa con el de su sede | Borra las 4 columnas (se pierden los horarios propios) |
| 09 | `2026-06-15_09_sede_lat_lng.sql` | Agrega `sede.latitud`/`longitud` como columnas simples y carga coordenadas de las sedes con id 6 a 10 | Borra las columnas |

### Por qué la 02 está obsoleta

La 02 se escribió cuando `Sede.java` calculaba `latitud`/`longitud` a partir del `Point` de PostGIS, así
que las columnas físicas eran "esquema muerto". Después la entidad pasó a usar esas columnas como fuente
de verdad (el cambio "gps fix") y la 09 las recreó. El resultado de aplicar 02 y luego 09 es el mismo que
no aplicar ninguna, pero **entre las dos** la tabla `sede` queda sin columnas que la entidad necesita.
Por eso no se aplica la 02 ni se vuelve a aplicar sola.

La 09 carga coordenadas con `UPDATE ... WHERE id = 6..10`: son los ids de las sedes de la base de
desarrollo del equipo. En una base nueva no existe ninguna de esas filas y la sentencia no modifica nada.

## Cómo aplicar a una base existente

Con la base levantada, aplicá las que falten, en orden, **sin** los `_rollback` y **sin** la 02:

```bash
for f in $(ls db/migrations/*.sql | grep -v -e _rollback -e _02_ | sort); do
  echo ">> $f"
  docker exec -i postgres_app psql -U postgres -d sport_db -v ON_ERROR_STOP=1 < "$f" || break
done
```

No uses `db/migrations/*.sql` a secas: incluye los `_rollback` y los ejecutaría. Como las migraciones
son idempotentes, volver a aplicar una ya aplicada es seguro.

Para revertir, aplicá el `_rollback.sql` correspondiente, empezando por la migración más nueva y
siguiendo hacia atrás. Revisá antes los avisos de la tabla (hay rollbacks que borran datos).

## Cómo agregar una migración nueva

La próxima es la **10**.

1. **Nombre:** `AAAA-MM-DD_NN_descripcion.sql` y `AAAA-MM-DD_NN_descripcion_rollback.sql`, con `NN` correlativo.
2. **Encabezado** con el contexto del cambio y sus riesgos.
3. **Idempotente:** `IF NOT EXISTS`, `IF EXISTS`, `WHERE ... IS NULL`, etc.
4. **`NOT NULL` sobre una tabla con filas:** `ADD COLUMN` nullable → backfill → `SET NOT NULL`.
5. **Datos en juego:** una guarda que aborte (`RAISE EXCEPTION`) si hay riesgo de perder información.
6. **Rollback propio**, con un aviso explícito si borra datos.
7. **Probarla en una base descartable** antes de abrir el PR:
   ```bash
   docker exec postgres_app psql -U postgres -c "CREATE DATABASE prueba_migracion;"
   # arrancar el backend apuntando a esa base, aplicar el script, volver a arrancar el backend
   # y confirmar que no hay errores de esquema
   docker exec postgres_app psql -U postgres -c "DROP DATABASE prueba_migracion WITH (FORCE);"
   ```
8. **Actualizar esta tabla** en el mismo PR.
9. **No editar migraciones ya aplicadas por otras personas.** Si hay que corregir algo, se agrega una
   migración nueva. La única excepción son los avisos en comentarios, como el de la 02.
