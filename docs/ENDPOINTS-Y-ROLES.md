# Endpoints y roles

Reglas de acceso del backend, definidas en `backend/.../config/Security.java`.
Las reglas están probadas en `backend/src/test/.../security/ReglasPorRolTest.java`:
si cambiás una regla o agregás un endpoint, actualizá el test y esta tabla.

Spring Security aplica la **primera regla que coincide**, en el orden en que están escritas.
Por eso las reglas de ADMIN sobre `/sedes`, `/canchas` y `/equipamiento` van **antes** de los GET públicos.

Roles: **Público** (sin token), **Usuario** (cualquier sesión válida: NO_SOCIO o SOCIO) y **ADMIN**.
Un anónimo que llega a un endpoint protegido recibe **403**; un usuario sin rol suficiente también.

## Públicos (sin token)

| Método | Ruta |
|---|---|
| POST | `/usuarios/login`, `/usuarios/register`, `/usuarios/verificar`, `/usuarios/reenviar-codigo`, `/usuarios/oauth/google` |
| POST | `/pagos/webhook` (lo llama Mercado Pago) |
| GET | `/sedes`, `/sedes/{id}` |
| GET | `/canchas`, `/canchas/buscar`, `/canchas/sede/{id}`, `/canchas/{id}` |
| GET | `/equipamiento` |
| GET | `/reservas/disponibilidad`, `/reservas/config` |
| GET | `/criticas/**` |
| — | `/ws/**` (WebSocket del chat; sin autenticar, ver T-06), `/error` |

## Cualquier usuario autenticado

| Método | Ruta | Nota |
|---|---|---|
| GET | `/geocoding/search`, `/geocoding/reverse`, `/geocoding/forward` | |
| GET | `/reservas/descuentos`, `/reservas/precio-preview` | |
| GET / PUT | `/usuarios/{id}`, `/usuarios/{id}/acreditar`, `/usuarios/{id}/cancelar-socio`, `/usuarios/{id}/preferencias` | ver "Brechas conocidas" |
| POST | `/criticas/canchas` | |
| GET / POST | `/chat/historial/{usuarioId}`, `/chat/leer/{usuarioId}` | ver "Brechas conocidas" |
| GET | `/reservas/usuario/{id}`, `/pagos/reserva/{id}` | además requieren ser dueño o ADMIN (T-03) |
| PUT / POST | `/reservas/{id}/cancelar`, `/pagos/iniciar`, `/pagos/confirmar`, `/pagos/tarjeta`, `/pagos/simular` | además requieren ser dueño (T-03) |

## Solo ADMIN

| Método | Ruta |
|---|---|
| GET | `/sedes/admin/**`, `/canchas/admin/**`, `/equipamiento/admin/**` |
| POST / PUT / DELETE | `/sedes/**`, `/canchas/**`, `/equipamiento/**` (alta, edición, baja, toggles, estado) |
| GET | `/usuarios`, `/usuarios/buscar` |
| PUT | `/usuarios/{id}/suspender`, `/usuarios/{id}/rehabilitar` |
| DELETE | `/usuarios/{id}` |
| POST | `/criticas/usuarios` |
| GET | `/reservas/usuario/{id}/sin-calificar-admin` |
| GET / POST | `/precios/**` |
| GET | `/reportes/**` |
| GET | `/chat/conversaciones` |

## Brechas conocidas (fuera de T-04)

Estos endpoints exigen sesión pero **no verifican que el recurso sea del usuario**, así que un usuario logueado
puede operar sobre otro cambiando el id de la URL. Es el mismo problema que T-03 corrige para reservas y pagos:

- `/usuarios/{id}` (GET, PUT), `/usuarios/{id}/acreditar`, `/usuarios/{id}/cancelar-socio`, `/usuarios/{id}/preferencias`.
- `/chat/historial/{usuarioId}` y `/chat/leer/{usuarioId}`.
- `GET /criticas/**` es público y expone las críticas por usuario (`/criticas/canchas/usuario/{usuarioId}`, `/criticas/usuarios/{id}`).
