package com.sportsync.backend.security;

import com.sportsync.backend.config.JwtUtil;
import com.sportsync.backend.model.admin.EstadoUsuario;
import com.sportsync.backend.model.entidades.Rol;
import com.sportsync.backend.model.entidades.Usuario;
import com.sportsync.backend.repository.CanchaRepository;
import com.sportsync.backend.repository.DescuentoHorarioRepository;
import com.sportsync.backend.repository.UsuarioRepository;
import com.sportsync.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * T-04: cada regla de acceso de {@code Security.java} (anónimo / usuario común / ADMIN).
 * Los servicios están mockeados: las llamadas de ADMIN que pasan la autorización llegan al
 * controlador pero no tocan la base, así que es seguro probar DELETE, PUT, etc.
 * (igual el contexto de Spring necesita Postgres arriba, como {@code BackendApplicationTests}).
 *
 * Qué se verifica: la AUTORIZACIÓN (401/403 o no). No importa qué código devuelva el controlador
 * con datos de prueba vacíos, solo que la regla deje o no pasar la petición.
 *
 * Si se agrega un endpoint nuevo o se cambia una regla, actualizar las listas de abajo y
 * docs/ENDPOINTS-Y-ROLES.md.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReglasPorRolTest {

    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwtUtil;

    @MockitoBean UsuarioRepository usuarioRepo;
    @MockitoBean CanchaRepository canchaRepo;
    @MockitoBean DescuentoHorarioRepository descuentoRepo;

    @MockitoBean SedeService sedeService;
    @MockitoBean CanchaService canchaService;
    @MockitoBean ItemEquipamientoService equipamientoService;
    @MockitoBean UsuarioService usuarioService;
    @MockitoBean CriticaService criticaService;
    @MockitoBean ReservaService reservaService;
    @MockitoBean PagoService pagoService;
    @MockitoBean PrecioInteligenteService precioService;
    @MockitoBean ReporteService reporteService;
    @MockitoBean ChatService chatService;
    @MockitoBean GeocodingService geocodingService;

    String tokenUsuario;
    String tokenAdmin;

    @BeforeEach
    void preparar() {
        when(usuarioRepo.findByEmail("user@test.com")).thenReturn(Optional.of(usuario(1L, "user@test.com", Rol.NO_SOCIO)));
        when(usuarioRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(usuario(99L, "admin@test.com", Rol.ADMIN)));
        tokenUsuario = jwtUtil.generarToken("user@test.com", "NO_SOCIO");
        tokenAdmin = jwtUtil.generarToken("admin@test.com", "ADMIN");
    }

    private static Usuario usuario(Long id, String email, Rol rol) {
        Usuario u = new Usuario();
        ReflectionTestUtils.setField(u, "id", id);
        u.setEmail(email);
        u.setRol(rol);
        u.setEstado(EstadoUsuario.ACTIVO);
        return u;
    }

    // ── Catálogo de reglas ────────────────────────────────────────────────────

    record Endpoint(HttpMethod metodo, String ruta) {
        @Override public String toString() { return metodo + " " + ruta; }
    }

    private static Endpoint get(String r)  { return new Endpoint(HttpMethod.GET, r); }
    private static Endpoint post(String r) { return new Endpoint(HttpMethod.POST, r); }
    private static Endpoint put(String r)  { return new Endpoint(HttpMethod.PUT, r); }
    private static Endpoint del(String r)  { return new Endpoint(HttpMethod.DELETE, r); }

    /** Solo ADMIN. */
    static Stream<Endpoint> soloAdmin() {
        return Stream.of(
                // sedes
                get("/sedes/admin/todas"), post("/sedes"), put("/sedes/1"), put("/sedes/1/toggle-activa"),
                post("/sedes/admin/geocodificar-todas"), del("/sedes/1"),
                // canchas
                get("/canchas/admin/sede/1"), post("/canchas/sede/1"), put("/canchas/1"),
                put("/canchas/1/estado"), del("/canchas/1"),
                // equipamiento
                get("/equipamiento/admin/todos"), post("/equipamiento"), put("/equipamiento/1"),
                put("/equipamiento/1/toggle"), del("/equipamiento/1"),
                // usuarios
                get("/usuarios"), get("/usuarios/buscar?email=a@b.com"),
                put("/usuarios/1/suspender"), put("/usuarios/1/rehabilitar"), del("/usuarios/1"),
                // críticas, reservas, precios, reportes, chat
                post("/criticas/usuarios"), get("/reservas/usuario/1/sin-calificar-admin"),
                get("/precios/descuentos-todos"), post("/precios/recalcular"),
                get("/reportes/reservas?mes=1&anio=2026"), get("/reportes/reservas/csv?mes=1&anio=2026"),
                get("/chat/conversaciones")
        );
    }

    /** Públicos: no requieren token. */
    static Stream<Endpoint> publicos() {
        return Stream.of(
                get("/sedes"), get("/sedes/1"),
                get("/canchas"), get("/canchas/buscar"), get("/canchas/sede/1"), get("/canchas/1"),
                get("/equipamiento"),
                get("/reservas/disponibilidad?canchaId=1&fecha=2026-12-01"), get("/reservas/config"),
                get("/criticas/canchas/1"),
                post("/usuarios/login"), post("/usuarios/register"), post("/usuarios/verificar"),
                post("/usuarios/reenviar-codigo"), post("/usuarios/oauth/google"),
                post("/pagos/webhook")
        );
    }

    /** Requieren sesión pero cualquier rol alcanza (sin chequeo de propiedad; eso es T-03 y otros tickets). */
    static Stream<Endpoint> cualquierUsuarioAutenticado() {
        return Stream.of(
                get("/geocoding/search?q=obelisco"),
                get("/reservas/descuentos?canchaId=1"),
                get("/reservas/precio-preview?canchaId=1&hora=18:00")
        );
    }

    /** Requieren sesión; con anónimo deben rechazarse (incluye endpoints con chequeo de propiedad). */
    static Stream<Endpoint> requierenSesion() {
        return Stream.concat(cualquierUsuarioAutenticado(), Stream.of(
                get("/usuarios/1"), put("/usuarios/1"), put("/usuarios/1/acreditar"),
                put("/usuarios/1/cancelar-socio"), put("/usuarios/1/preferencias"),
                get("/reservas/usuario/1"), put("/reservas/1/cancelar"),
                post("/pagos/iniciar"), post("/pagos/confirmar?reservaId=1"),
                post("/pagos/tarjeta?reservaId=1"), post("/pagos/simular?reservaId=1&resultado=APROBADO"),
                get("/pagos/reserva/1"),
                post("/criticas/canchas"), get("/chat/historial/1"), post("/chat/leer/1")
        ));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int estado(Endpoint e, String token) {
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.request(e.metodo(), e.ruta());
        if (e.metodo() != HttpMethod.GET && e.metodo() != HttpMethod.DELETE) {
            req.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        if (token != null) req.header("Authorization", "Bearer " + token);
        try {
            return mvc.perform(req).andReturn().getResponse().getStatus();
        } catch (Exception ex) {
            // Una excepción del controlador (ej. NPE con datos mockeados vacíos) significa que la
            // petición PASÓ la autorización y llegó a ejecutarse.
            return 500;
        }
    }

    private static boolean denegado(int status) {
        return status == 401 || status == 403;
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "anónimo rechazado en {0}")
    @MethodSource("soloAdmin")
    void soloAdmin_anonimo_denegado(Endpoint e) {
        int s = estado(e, null);
        assertTrue(denegado(s), e + " debía rechazar al anónimo pero devolvió " + s);
    }

    @ParameterizedTest(name = "usuario común rechazado en {0}")
    @MethodSource("soloAdmin")
    void soloAdmin_usuario_comun_403(Endpoint e) {
        int s = estado(e, tokenUsuario);
        assertTrue(s == 403, e + " debía devolver 403 a un usuario no admin pero devolvió " + s);
    }

    @ParameterizedTest(name = "admin permitido en {0}")
    @MethodSource("soloAdmin")
    void soloAdmin_admin_permitido(Endpoint e) {
        int s = estado(e, tokenAdmin);
        assertTrue(!denegado(s), e + " debía dejar pasar a ADMIN pero devolvió " + s);
    }

    @ParameterizedTest(name = "público: {0}")
    @MethodSource("publicos")
    void publicos_sin_token_permitido(Endpoint e) {
        int s = estado(e, null);
        // Solo se mira el 403: es el que devuelve Spring Security a un anónimo. Un 401 puede venir
        // del propio controlador (ej. /usuarios/login con credenciales vacías) y no es un bloqueo.
        assertTrue(s != 403, e + " debía ser público pero Security lo bloqueó (403)");
    }

    @ParameterizedTest(name = "anónimo rechazado en {0}")
    @MethodSource("requierenSesion")
    void requierenSesion_anonimo_denegado(Endpoint e) {
        int s = estado(e, null);
        assertTrue(denegado(s), e + " debía exigir sesión pero devolvió " + s);
    }

    @ParameterizedTest(name = "usuario común permitido en {0}")
    @MethodSource("cualquierUsuarioAutenticado")
    void cualquierUsuario_permitido(Endpoint e) {
        int s = estado(e, tokenUsuario);
        assertTrue(!denegado(s), e + " debía dejar pasar a un usuario común pero devolvió " + s);
    }
}
