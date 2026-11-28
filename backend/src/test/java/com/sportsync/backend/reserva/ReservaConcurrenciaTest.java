package com.sportsync.backend.reserva;

import com.sportsync.backend.config.JwtUtil;
import com.sportsync.backend.model.admin.EstadoReserva;
import com.sportsync.backend.model.cancha.Cancha;
import com.sportsync.backend.model.entidades.Rol;
import com.sportsync.backend.model.entidades.Usuario;
import com.sportsync.backend.model.reserva.Reserva;
import com.sportsync.backend.model.sede.Sede;
import com.sportsync.backend.repository.CanchaRepository;
import com.sportsync.backend.repository.ReservaRepository;
import com.sportsync.backend.repository.SedeRepository;
import com.sportsync.backend.repository.UsuarioRepository;
import com.sportsync.backend.service.ReservaService;
import com.sportsync.backend.testsupport.BaseDeDatosTemporal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * T-01: dos o más pedidos simultáneos por el mismo turno no pueden crear dos reservas; el que pierde
 * recibe 409 (no 500). Se prueba con pedidos HTTP reales a POST /pagos/iniciar, contra una base temporal
 * con el esquema completo (lo crea Hibernate) y la migración 10 aplicada. No toca sport_db.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReservaConcurrenciaTest {

    private static final BaseDeDatosTemporal BD = BaseDeDatosTemporal.crear("t01conc");
    private static final int USUARIOS = 8;
    private static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");

    @DynamicPropertySource
    static void baseTemporal(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", BD::url);
        r.add("spring.datasource.username", BD::usuario);
        r.add("spring.datasource.password", BD::clave);
    }

    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwtUtil;
    @Autowired SedeRepository sedeRepo;
    @Autowired CanchaRepository canchaRepo;
    @Autowired UsuarioRepository usuarioRepo;
    @Autowired ReservaRepository reservaRepo;
    @Autowired ReservaService reservaService;

    private Long canchaId;
    private final List<Usuario> usuarios = new ArrayList<>();

    /** Corre con el contexto ya cargado (las tablas existen): siembra datos y aplica la migración 10. */
    @BeforeAll
    void sembrar() throws Exception {
        BD.ejecutarArchivo(BaseDeDatosTemporal.migracion("2026-11-28_10_reserva_sin_solapamiento.sql"));

        Sede sede = new Sede();
        sede.setNombre("Sede de prueba");
        sede.setDireccion("Calle 123");
        sede.setHoraApertura("08:00");
        sede.setHoraCierre("23:00");
        sede = sedeRepo.save(sede);

        Cancha cancha = new Cancha();
        cancha.setNombre("Cancha 1");
        cancha.setTipo(5);
        cancha.setPrecioBase(10000);
        cancha.setSede(sede);
        canchaId = canchaRepo.save(cancha).getId();

        for (int i = 1; i <= USUARIOS; i++) {
            Usuario u = new Usuario();
            u.setNombre("Usuario " + i);
            u.setEmail("usuario" + i + "@prueba.com");
            u.setPassword("no-importa");
            u.setDni("1000000" + i);
            u.setTelefono("1100000" + i);
            u.setRol(Rol.NO_SOCIO);
            u.setVerificado(true);
            usuarios.add(usuarioRepo.save(u));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String fecha(int diasDesdeHoy) {
        return LocalDate.now(BUENOS_AIRES).plusDays(diasDesdeHoy).toString();
    }

    private String body(Usuario u, String fecha, String inicio, String fin) {
        return "{\"reserva\":{\"usuarioId\":" + u.getId() + ",\"canchaId\":" + canchaId
                + ",\"fecha\":\"" + fecha + "\",\"horaInicio\":\"" + inicio + ":00\",\"horaFin\":\"" + fin + ":00\""
                + ",\"iluminacion\":false},\"metodo\":\"TARJETA_CREDITO\"}";
    }

    private MvcResult iniciarPago(Usuario u, String fecha, String inicio, String fin) throws Exception {
        String token = jwtUtil.generarToken(u.getEmail(), u.getRol().name());
        return mvc.perform(post("/pagos/iniciar")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(u, fecha, inicio, fin))).andReturn();
    }

    private long reservasVigentes(String fecha, String inicio) {
        return reservaRepo.findAll().stream()
                .filter(r -> r.getCancha().getId().equals(canchaId))
                .filter(r -> r.getFecha().toString().equals(fecha))
                .filter(r -> r.getHoraInicio().toString().startsWith(inicio))
                .filter(r -> r.getEstado() == EstadoReserva.ACTIVA || r.getEstado() == EstadoReserva.PENDIENTE_PAGO)
                .count();
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void ocho_pedidos_simultaneos_al_mismo_turno_crean_una_sola_reserva() throws Exception {
        String dia = fecha(2);
        ExecutorService pool = Executors.newFixedThreadPool(USUARIOS);
        CountDownLatch listos = new CountDownLatch(USUARIOS);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> resultados = new ArrayList<>();
        try {
            for (Usuario u : usuarios) {
                resultados.add(pool.submit(() -> {
                    listos.countDown();
                    largada.await();   // todos arrancan juntos
                    return iniciarPago(u, dia, "20:00", "21:00").getResponse().getStatus();
                }));
            }
            assertTrue(listos.await(30, TimeUnit.SECONDS));
            largada.countDown();

            int ok = 0, conflicto = 0;
            List<Integer> otros = new ArrayList<>();
            for (Future<Integer> f : resultados) {
                int status = f.get(60, TimeUnit.SECONDS);
                if (status == 200) ok++;
                else if (status == 409) conflicto++;
                else otros.add(status);
            }
            assertEquals(1, ok, "exactamente un pedido debe ganar el turno");
            assertEquals(USUARIOS - 1, conflicto, "los demás deben recibir 409");
            assertTrue(otros.isEmpty(), "ningún pedido debe terminar en 500 u otro código: " + otros);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, reservasVigentes(dia, "20:00"), "en la base debe haber una sola reserva para el turno");
    }

    @Test
    void un_pedido_posterior_sobre_un_turno_ocupado_tambien_recibe_409_con_mensaje() throws Exception {
        String dia = fecha(3);
        assertEquals(200, iniciarPago(usuarios.get(0), dia, "18:00", "19:00").getResponse().getStatus());

        MvcResult segundo = iniciarPago(usuarios.get(1), dia, "18:00", "19:00");
        assertEquals(409, segundo.getResponse().getStatus());
        assertTrue(segundo.getResponse().getContentAsString().contains("ya fue reservado"),
                segundo.getResponse().getContentAsString());
    }

    @Test
    void un_pedido_que_se_superpone_solo_en_parte_tambien_recibe_409() throws Exception {
        String dia = fecha(4);
        assertEquals(200, iniciarPago(usuarios.get(0), dia, "18:00", "19:00").getResponse().getStatus());
        assertEquals(409, iniciarPago(usuarios.get(1), dia, "18:30", "19:30").getResponse().getStatus());
    }

    @Test
    void turnos_contiguos_no_chocan() throws Exception {
        String dia = fecha(5);
        assertEquals(200, iniciarPago(usuarios.get(0), dia, "17:00", "18:00").getResponse().getStatus());
        assertEquals(200, iniciarPago(usuarios.get(1), dia, "18:00", "19:00").getResponse().getStatus());
    }

    @Test
    void si_la_reserva_se_cancela_el_turno_se_puede_volver_a_reservar() throws Exception {
        String dia = fecha(6);
        assertEquals(200, iniciarPago(usuarios.get(0), dia, "19:00", "20:00").getResponse().getStatus());
        assertEquals(409, iniciarPago(usuarios.get(1), dia, "19:00", "20:00").getResponse().getStatus());

        Reserva primera = reservaRepo.findAll().stream()
                .filter(r -> r.getFecha().toString().equals(dia) && r.getHoraInicio().toString().startsWith("19:00"))
                .findFirst().orElseThrow();
        reservaService.cancelarPorPago(primera);   // pago rechazado/vencido → CANCELADA

        assertEquals(200, iniciarPago(usuarios.get(1), dia, "19:00", "20:00").getResponse().getStatus());
    }

    @Test
    void una_hora_de_fin_igual_a_la_de_inicio_es_un_error_del_cliente_no_del_servidor() throws Exception {
        MvcResult r = iniciarPago(usuarios.get(0), fecha(7), "18:00", "18:00");
        assertEquals(400, r.getResponse().getStatus(), r.getResponse().getContentAsString());
    }
}
