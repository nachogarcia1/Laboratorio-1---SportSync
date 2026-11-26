# SportSync: relevamiento técnico y backlog

Relevamiento de solo lectura del repositorio. Este documento contiene el diagnóstico, el flujo de trabajo y el plan de implementación en tickets.

---

# Diagnóstico

## 1. Arquitectura
- Monorepo con `backend/`, `frontend/`, `db/` y `docker-compose.yml`. El `package.json` de la raíz es un residuo (solo `react-router-dom`, mezcla npm con el pnpm del frontend).
- **Backend:** Spring Boot 4.0.5, Maven, Java 17 en `pom.xml` y 21 en el Dockerfile (inconsistente).
  - Dependencias: JPA, Security + JWT (jjwt), WebSocket STOMP, Mail, Hibernate Spatial/PostGIS y Mercado Pago SDK.
  - Capas: `config`, `controller` (11), `service`, `repository` (14), `dto`, `model`.
- **Frontend:** React 19 + Vite 8, JavaScript sin TypeScript, pnpm, `react-router-dom`, STOMP, Leaflet. Sin librería de estado ni cliente HTTP (usa `fetch`).
  - `Admin.jsx` tiene 1648 líneas y `SedeDetalle.jsx` 575.
  - `HacerReserva.jsx` es código muerto.
  - No hay ruta 404.
- **Datos:** solo PostgreSQL 16 + PostGIS.
  - Migraciones SQL manuales en `db/migrations` (9 scripts) combinadas con `ddl-auto=update`.
  - No hay base no relacional, aunque el README la declara.

## 2. Infraestructura
- `docker-compose.yml` levanta `db` (postgis 16-3.4, puerto 5434), `backend` (8080) y `frontend` (5173).
- El frontend en Docker corre el servidor de desarrollo de Vite, no un build de producción.
- No hay healthchecks, el `depends_on` no espera a que la base esté lista, y no hay cloud ni IaC.
- El compose **no pasa** `JWT_SECRET`, `MAIL_*`, `GOOGLE_OAUTH_CLIENT_ID` ni `MP_*` al contenedor del backend (no hay `env_file`).
- Las credenciales de la base están en texto plano en el compose y en `application.properties`.
- Servicios externos: SMTP (Gmail), Google OAuth, Mercado Pago (más ngrok para el webhook) y Nominatim/OSM.

## 3. Configuración y automatización
- **Variables del backend:** `MAIL_HOST/PORT/USERNAME/PASSWORD/FROM`, `GOOGLE_OAUTH_CLIENT_ID`, `JWT_SECRET`, `MP_ACCESS_TOKEN`, `MP_WEBHOOK_URL`, `FRONTEND_URL`, `PAGOS_SIMULACION`, `VERIF_DEV_LOG` y `SPRING_DATASOURCE_*`.
  - `backend/.env.example` solo documenta una parte: faltan `MP_*`, `FRONTEND_URL`, `PAGOS_SIMULACION` y `VERIF_DEV_LOG`.
  - `JWT_SECRET` tiene un valor por defecto hardcodeado en `application.properties`.
- **Variables del frontend:** solo `VITE_GOOGLE_CLIENT_ID`. No hay `frontend/.env.example`.
  - `API_URL` está hardcodeada a `http://localhost:8080` en `utils/api.js:1`, y el WebSocket (`Chat.jsx:28`, `Admin.jsx:300`) y el CSV (`Admin.jsx:664`) también.
- **Git hooks:** ninguno. `.github/` solo tiene artefactos de una extensión de IDE (java-upgrade).
- **CI/CD:** no hay `.github/workflows/`. No hay build, lint, tests ni deploy automáticos.
- **Calidad:** ESLint 9 en el frontend (sin Prettier). El backend tiene 24 tests unitarios, y el Dockerfile compila con `-DskipTests`. El frontend no tiene tests.

## 4. Estado actual vs. requerimientos (README)

| Requerimiento | Estado |
|---|---|
| Registro, login, verificación de email, Google OAuth | Hecho |
| Socios / no socios (descuento 10%, límites de reservas y anticipación) | Hecho. La membresía nunca vence (falta job). |
| Consulta de disponibilidad "en tiempo real" | Parcial. Consulta bajo demanda, sin polling ni push. |
| Reserva con control de concurrencia | **Faltante.** Race condition entre `existeSolapamiento` y `save`. |
| Precio dinámico | Hecho (`PrecioInteligenteService`). Usa horario global 8–22 y constantes hardcodeadas. |
| Servicios adicionales (equipamiento) | Parcial. El stock se valida pero no se descuenta ni se repone. |
| Panel admin (sedes, canchas, reservas, usuarios) | Hecho. Es un único archivo monolítico. |
| Pagos | Mock + Mercado Pago opcional. Los pendientes no expiran y el webhook no valida firma. |
| Recordatorios | Solo el flag `recibirRecordatorios`. No se encontró ningún envío (hay que confirmarlo en el código de los schedulers). |
| GPS y mapa | Hecho |
| Chat de soporte | Hecho. El WebSocket no está autenticado. |
| Bases relacionales **y no relacionales** | **Faltante.** Solo existe PostgreSQL. |

## 5. Riesgos de seguridad y corrección detectados
1. **Race condition en reservas.** Sin lock, `@Version`, constraint única ni exclusion constraint. `PENDIENTE_PAGO` no bloquea el turno, y al confirmar no se revalida.
2. **IDOR.** `POST /reservas`, `GET /reservas/usuario/{id}`, `PUT /reservas/{id}/cancelar` (con `usuarioId` en el body) y los endpoints de pagos toman el id del cliente sin compararlo con el JWT.
3. **`PUT /equipamiento/{id}` y `/{id}/toggle` no exigen ADMIN.** Caen en `anyRequest().authenticated()`.
4. **`JWT_SECRET` por defecto** en el repo, y credenciales de DB en texto plano.
5. **`POST /reservas` crea reservas ACTIVAS sin pago.** Convive con `/pagos/iniciar`. Decidido: se elimina (T-24).
6. **WebSocket `/ws` público**, y el cliente no envía el token.
7. **`/pagos/simular` queda habilitado** cuando falta el token de Mercado Pago, y el webhook de MP no valida firma.
8. **Sin `@ControllerAdvice`.** Los códigos de error son inconsistentes (ej. 404 para cualquier excepción en `POST /reservas`).
9. **Migraciones inconsistentes.** `ddl-auto=update` convive con scripts manuales. Latitud/longitud se eliminan en la 02 y se reagregan en la 09. El `README` de migraciones solo lista la 01 y la 02.

---

# Flujo de trabajo (GitHub Flow)

Regla general: **un ticket = una rama = un PR**. Este flujo se aplica a todos los tickets, incluido T-00. T-00 lo formaliza en el repo.

1. `main` siempre está estable y desplegable. No se commitea ni se pushea directo a `main`.
2. Antes de empezar un ticket: `git switch main && git pull --ff-only`, y luego `git switch -c <tipo>/T-XX-slug`.
   - Tipos: `feat`, `fix`, `chore`, `docs`, `test`, `refactor`, `ci`.
   - Ejemplos: `chore/T-00-workflow`, `fix/T-03-idor-reservas`, `feat/T-10-recordatorios`.
3. Commits pequeños y atómicos, con Conventional Commits y el id del ticket: `fix(reservas): validar dueño en cancelar [T-03]`. El repo ya usa el prefijo `fix:`, así que la convención es continuidad.
4. Push de la rama al remoto (`git push -u origin <rama>`) apenas haya un primer commit, para tener trazabilidad. Prohibido `--force` sobre `main`. En ramas propias solo `--force-with-lease`.
5. Abrir un PR contra `main` (puede ser borrador) con la plantilla: ticket, qué cambia, cómo se probó y checklist de criterios de aceptación.
6. Merge solo con revisión y checks en verde (cuando exista T-16). Estrategia de merge: **squash**, con el mensaje final con el id del ticket, y se borra la rama.
7. Después del merge: `git switch main && git pull --ff-only`, y recién entonces se arranca el siguiente ticket.
8. Los tickets con dependencias solo arrancan cuando sus dependencias ya están mergeadas en `main`. No se apilan ramas sobre ramas sin mergear.

---

# Tickets (ordenados por prioridad y dependencias)

Convención: **P0** crítico/seguridad, **P1** requisito faltante, **P2** infra y calidad, **P3** mejoras. **T-00** es prerrequisito de todos los demás.

## P0: Proceso, seguridad y corrección

### T-00 Workflow de commit y push (GitHub Flow)
- **Objetivo:** dejar definido y documentado en el repo cómo se ramifica, se commitea, se pushea y se mergea, para tener trazabilidad ticket por ticket.
- **Área:** Infra / Docs.
- **Criterios de aceptación:**
  - `CONTRIBUTING.md` en la raíz con el flujo de la sección "Flujo de trabajo": nomenclatura de ramas, formato de commits (Conventional Commits + id de ticket), reglas de push y estrategia de merge.
  - `.github/pull_request_template.md` con ticket vinculado, descripción, cómo se probó y checklist de criterios de aceptación.
  - Reglas de protección de `main` configuradas en GitHub (acción manual del usuario, documentada en `CONTRIBUTING.md`): PR obligatorio, sin push directo, sin force-push, rama actualizada antes de mergear y borrado automático de ramas mergeadas. Los checks obligatorios se suman cuando exista T-16.
  - La convención de mensajes de commit queda documentada con ejemplos. Un hook `commit-msg` que la valide es opcional y va en T-17.
  - El propio T-00 se entrega en la rama `chore/T-00-workflow` mediante un PR, como primer caso del flujo.
- **Dependencias:** ninguna. Es el primer ticket que se trabaja.

### T-24 Eliminar `POST /reservas` directo
- **Decisión:** se elimina el endpoint. Toda reserva se crea por el flujo con pago (`/pagos/iniciar`).
- **Objetivo:** que no existan reservas ACTIVAS creadas sin pasar por el pago.
- **Área:** Backend / Frontend.
- **Criterios de aceptación:**
  - Se elimina `POST /reservas` (controlador, regla en `Security.java` y DTO si queda sin uso). La lógica de `ReservaService.crearReservaConEstado` que usa el flujo de pago se conserva.
  - Se verifica que el frontend no lo usa: se revisa `HacerReserva.jsx` y `SedeDetalle.jsx` (si alguno lo llama, pasa al flujo de pago).
  - Se actualizan o eliminan los tests y el archivo `usuario.http` que lo usen.
  - El flujo completo de reserva con pago sigue funcionando (prueba manual y tests existentes).
- **Dependencias:** T-00.

### T-01 Control de concurrencia en reservas
- **Objetivo:** que dos requests simultáneos no puedan reservar el mismo turno.
- **Área:** Backend / DB.
- **Criterios de aceptación:**
  - Existe una constraint de exclusión (o equivalente) sobre cancha, fecha y rango horario para reservas `ACTIVA` y `PENDIENTE_PAGO`.
  - El segundo intento concurrente recibe 409 y no 500.
  - Un test de concurrencia con N hilos sobre el mismo turno termina con exactamente 1 reserva.
  - Hay un script de migración con su rollback en `db/migrations`.
- **Dependencias:** T-00.

### T-02 Bloqueo temporal de turnos con expiración de `PENDIENTE_PAGO`
- **Objetivo:** que una reserva pendiente de pago bloquee el turno y libere el cupo si no se paga.
- **Área:** Backend.
- **Criterios de aceptación:**
  - `existeSolapamiento` considera `PENDIENTE_PAGO`.
  - Un job programado pasa a `VENCIDO`/`CANCELADA` las reservas y pagos pendientes pasado un TTL configurable.
  - Al confirmar el pago se revalida que el turno siga disponible.
  - Tests del job y de la revalidación.
- **Dependencias:** T-00, T-01.

### T-03 Autorización por dueño (corregir IDOR)
- **Objetivo:** que el usuario autenticado solo opere sobre sus propios recursos.
- **Área:** Backend.
- **Criterios de aceptación:**
  - `/reservas` (listar por usuario, cancelar) y `/pagos/*` toman el usuario del JWT. Si el id recibido no coincide, responde 403, salvo para ADMIN.
  - Tests de seguridad que prueban acceso cruzado entre dos usuarios.
- **Dependencias:** T-00, T-24.

### T-04 Cerrar huecos de autorización
- **Objetivo:** que `PUT /equipamiento/{id}` y `/{id}/toggle` exijan ADMIN, y revisar el resto de reglas.
- **Área:** Backend.
- **Criterios de aceptación:**
  - Los endpoints de escritura de equipamiento devuelven 403 a un usuario no admin.
  - Se elimina la regla duplicada de `/canchas/buscar`.
  - Hay una tabla endpoint × rol documentada y un test de cada regla.
- **Dependencias:** T-00.

### T-05 Gestión de secretos y configuración segura
- **Objetivo:** sacar secretos y valores por defecto inseguros del repo.
- **Área:** Backend / Infra.
- **Criterios de aceptación:**
  - `JWT_SECRET` no tiene default: la app falla al arrancar si falta (salvo en el perfil `dev`).
  - Las credenciales de la base salen de variables de entorno, no del compose ni de `application.properties`.
  - `backend/.env.example` documenta todas las variables (`MP_*`, `FRONTEND_URL`, `PAGOS_SIMULACION`, `VERIF_DEV_LOG`).
  - `PAGOS_SIMULACION` y `/pagos/simular` quedan limitados al perfil `dev`.
- **Dependencias:** T-00.

### T-06 Autenticar el WebSocket y validar el webhook de pagos
- **Objetivo:** que solo usuarios autenticados usen el chat, y que el webhook de Mercado Pago sea verificable.
- **Área:** Backend / Frontend.
- **Criterios de aceptación:**
  - El handshake o `CONNECT` STOMP valida el JWT, y el cliente envía el token en `connectHeaders`.
  - Un usuario no puede suscribirse al topic de otro usuario.
  - El webhook valida la firma de Mercado Pago, o descarta notificaciones cuyo estado no coincida con el consultado a la API.
- **Dependencias:** T-00, T-03.

### T-07 Manejo global de errores
- **Objetivo:** respuestas de error consistentes.
- **Área:** Backend.
- **Criterios de aceptación:**
  - Hay un `@ControllerAdvice` con cuerpo de error uniforme.
  - Se usan 400, 401, 403, 404, 409 y 422 donde corresponde, y ningún controller convierte excepciones genéricas en 404.
  - El frontend sigue funcionando con el nuevo formato (`apiFetch`).
- **Dependencias:** T-00, T-01, T-03.

## P1: Requisitos del README sin cumplir

### T-08 Base de datos no relacional (MongoDB)
- **Decisión:** el motor es MongoDB.
- **Objetivo:** incorporar la base no relacional que declara el README. El caso de uso inicial es el chat de soporte (`mensaje_chat`), que hoy está en Postgres. El caso de uso queda a confirmar al empezar el ticket.
- **Área:** Backend / Infra.
- **Criterios de aceptación:**
  - Hay un servicio `mongo` en `docker-compose.yml` con healthcheck y credenciales por variables de entorno (documentadas en `.env.example`).
  - El backend incorpora `spring-boot-starter-data-mongodb`, y los mensajes del chat se guardan y se leen desde MongoDB, con una capa de repositorio desacoplada.
  - Los endpoints `/chat/*` y el WebSocket siguen funcionando igual para el frontend.
  - Los mensajes existentes en Postgres se descartan (son de prueba), y no se escribe migración de datos. La entidad `MensajeChat` y la tabla `mensaje_chat` se retiran de Postgres, con un script SQL manual y su rollback en `db/migrations`.
  - Hay documentación de qué datos van en cada base y por qué.
  - Tests del repositorio con Testcontainers para MongoDB.
- **Dependencias:** T-00, T-05.

### T-09 Disponibilidad en tiempo real
- **Objetivo:** que la grilla de turnos se actualice sin acción del usuario.
- **Área:** Backend / Frontend.
- **Criterios de aceptación:**
  - Se publica un evento por STOMP (`/topic/disponibilidad/{canchaId}`) al crear o cancelar una reserva.
  - `SedeDetalle` se suscribe y actualiza la grilla, con polling como respaldo.
  - Si dos usuarios ven el mismo turno, cuando uno reserva el otro lo ve ocupado en pocos segundos.
- **Dependencias:** T-00, T-01, T-06.

### T-10 Recordatorios de reservas
- **Objetivo:** enviar recordatorios a quienes tienen `recibirRecordatorios = true`.
- **Área:** Backend.
- **Criterios de aceptación:**
  - Un scheduler envía un email antes del turno (anticipación configurable), solo a usuarios con el flag activo.
  - No se envía dos veces el mismo recordatorio.
  - Se respeta la zona horaria de la sede, y hay tests con `Clock` fijo.
- **Dependencias:** T-00. **Verificar primero** que no exista ya una implementación parcial en los schedulers.

### T-11 Vencimiento de membresías
- **Objetivo:** que el rol SOCIO caduque.
- **Área:** Backend.
- **Criterios de aceptación:**
  - Un job diario pasa a NO_SOCIO las membresías con `fechaVencimiento` vencida y las marca como vencidas.
  - Se avisa por email antes del vencimiento (opcional).
  - Tests del job.
- **Dependencias:** T-00.

### T-12 Stock de equipamiento coherente
- **Objetivo:** que el stock refleje las reservas.
- **Área:** Backend.
- **Criterios de aceptación:**
  - El stock se descuenta al confirmar la reserva y se repone al cancelarla o vencer.
  - La validación es segura ante concurrencia (lock o actualización atómica).
  - Los tests cubren los casos de reserva, cancelación y carrera.
- **Dependencias:** T-00, T-01, T-02.

## P2: Infraestructura, CI y calidad

### T-13 Ordenar las migraciones SQL manuales
- **Decisión:** se mantienen los scripts manuales en `db/migrations` (no se adopta Flyway).
- **Objetivo:** que los scripts manuales sean consistentes, estén documentados y permitan levantar una base desde cero.
- **Área:** Backend / DB.
- **Criterios de aceptación:**
  - `db/migrations/README.md` lista los 9 scripts actuales, con su orden, su rollback y la convención para agregar nuevos (numeración, un rollback por script).
  - Se resuelve la inconsistencia de latitud/longitud en `sede` (la 02 las elimina, la 09 las reagrega y `Sede.java` las usa).
  - Hay un procedimiento documentado y probado para levantar una base vacía con el esquema completo.
  - Se define el rol de `ddl-auto` junto con los scripts (documentado en el README de migraciones) para evitar divergencias entre Hibernate y el SQL.
- **Dependencias:** T-00 (hacerlo antes de T-01 si se quiere evitar conflictos de esquema).

### T-14 Docker Compose robusto
- **Objetivo:** que el entorno local levante bien y de forma reproducible.
- **Área:** Infra.
- **Criterios de aceptación:**
  - Healthcheck en `db` y `backend`, y `depends_on` con `condition: service_healthy`.
  - `env_file` o `environment` pasa al backend todas las variables requeridas.
  - Una versión de Java única entre `pom.xml` y el Dockerfile.
  - Un `docker compose up` desde cero funciona siguiendo solo el README.
- **Dependencias:** T-00, T-05.

### T-15 Frontend configurable por entorno y build de producción
- **Objetivo:** quitar los hardcodeos de `localhost` y servir un build real.
- **Área:** Frontend / Infra.
- **Criterios de aceptación:**
  - `VITE_API_URL` y `VITE_WS_URL` reemplazan los usos de `localhost:8080` (`api.js`, `Chat.jsx`, `Admin.jsx`, CSV).
  - Existe `frontend/.env.example`.
  - Un Dockerfile de producción (build + nginx) y un servicio compose para él, además del modo dev.
  - Se elimina `HacerReserva.jsx` (código muerto).
- **Dependencias:** T-00.

### T-16 Pipeline de CI en GitHub Actions
- **Objetivo:** validar cada PR automáticamente.
- **Área:** Infra.
- **Criterios de aceptación:**
  - Un workflow en PR y push a `main` con jobs de backend (`mvn verify` con Postgres/PostGIS como service, tests habilitados) y de frontend (`pnpm install`, lint y build).
  - Caché de dependencias.
  - Los jobs fallan ante errores, y se agregan como checks obligatorios en la protección de `main` definida en T-00 (a configurar en GitHub).
  - Se quita `-DskipTests` del build de CI.
- **Dependencias:** T-00, T-14, T-15.

### T-17 Hooks de git y formato
- **Objetivo:** frenar errores antes del commit.
- **Área:** Frontend / Infra.
- **Criterios de aceptación:**
  - Husky (o equivalente) con `lint-staged` para ESLint en el frontend.
  - Un hook `commit-msg` que valida el formato de commits definido en T-00 (Conventional Commits + id de ticket).
  - Prettier configurado (opcional), con una nota en el README sobre cómo instalar los hooks.
  - Se limpia el `package.json` y el `package-lock.json` de la raíz (o se convierten en el workspace real).
- **Dependencias:** T-00, T-15.

### T-18 Tests de backend para el núcleo del negocio
- **Objetivo:** cubrir lo que hoy no tiene tests.
- **Área:** Backend.
- **Criterios de aceptación:**
  - Tests de `ReservaService` (reglas de socio y no socio, antelación, límites, precio con iluminación y equipamiento).
  - Tests de `PrecioInteligenteService`.
  - Tests de seguridad por rol (MockMvc).
  - `BackendApplicationTests` corre contra Testcontainers y no requiere una base externa.
- **Dependencias:** T-00, T-03, T-04.

### T-19 Tests de frontend
- **Objetivo:** tener una base de tests.
- **Área:** Frontend.
- **Criterios de aceptación:**
  - Vitest + Testing Library con un script `test`, ejecutado en CI.
  - Tests de `apiFetch` (401, errores) y de las rutas protegidas (`ProtectedRoute`, `AdminRoute`).
  - Al menos un test del flujo de reserva con la API mockeada.
- **Dependencias:** T-00, T-16.

## P3: Mejoras

### T-20 Contexto de autenticación y 404
- **Objetivo:** centralizar la sesión.
- **Área:** Frontend.
- **Criterios de aceptación:**
  - Un `AuthContext` con login, logout y expiración del token. `ProtectedRoute` valida la expiración.
  - Una ruta comodín 404.
  - Las páginas dejan de leer `sessionStorage` directamente.
- **Dependencias:** T-00, T-15.

### T-21 Dividir `Admin.jsx`
- **Objetivo:** mantenibilidad del panel admin.
- **Área:** Frontend.
- **Criterios de aceptación:**
  - `Admin.jsx` queda como contenedor, y cada sección (usuarios, sedes, canchas, equipamiento, precios, reportes, chat) pasa a su propio componente, sin cambios de comportamiento.
  - Los estilos inline pasan a CSS.
- **Dependencias:** T-00, T-19 (para tener una red de seguridad).

### T-22 Parametrizar constantes de negocio y horarios por cancha
- **Objetivo:** que el negocio se configure sin recompilar.
- **Área:** Backend.
- **Criterios de aceptación:**
  - El 10% de socio, el recargo de iluminación, los límites y `max-fraccion-descuento` y `sportsync.socio.cuota` están en `application.properties` y documentados.
  - `PrecioInteligenteService` genera los slots con el horario y los días de cada cancha, no con 8–22 fijo.
  - Se corrige el nombre engañoso `peakCount`.
- **Dependencias:** T-00, T-18.

### T-23 Documentación
- **Objetivo:** que un compañero pueda levantar y entender el proyecto.
- **Área:** Infra / Docs.
- **Criterios de aceptación:**
  - Un README con requisitos, setup (compose y local), variables, tests, arquitectura y la tabla endpoint × rol.
  - Un `CLAUDE.md` con los comandos y las convenciones.
  - Se corrige la afirmación sobre bases no relacionales según lo que quede implementado en T-08.
- **Dependencias:** T-00, T-08, T-14, T-16.

---

## Orden sugerido
Todos los tickets dependen de T-00 y se trabajan de a uno por rama, siguiendo el flujo de arriba. Las agrupaciones indican qué tickets pueden elegirse en cualquier orden entre sí, no que se trabajen en paralelo sobre la misma rama.
0. **Primero:** T-00.
1. **Luego:** T-24 (antes que T-03), T-03, T-04, T-05, T-13.
2. **Después:** T-01 → T-02 → T-12, y T-14, T-15.
3. **Más tarde:** T-06, T-07, T-16, T-09, T-10, T-11.
4. **Al final:** T-08, T-17, T-18, T-19, T-20, T-21, T-22, T-23.

## Decisiones tomadas
- **Migraciones:** se mantienen los scripts SQL manuales, sin Flyway (T-13).
- **Base no relacional:** MongoDB, con el chat de soporte como caso de uso inicial (T-08).
- **`POST /reservas` directo:** se elimina, y queda solo el flujo con pago (T-24).
- **Mensajes de chat existentes:** se descartan al pasar a MongoDB, porque son de prueba (T-08).

## Decisiones abiertas
- Ninguna por ahora.
