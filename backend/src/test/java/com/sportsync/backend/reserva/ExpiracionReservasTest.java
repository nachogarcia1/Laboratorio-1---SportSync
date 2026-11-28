package com.sportsync.backend.reserva;

import com.sportsync.backend.config.JwtUtil;
import com.sportsync.backend.model.admin.EstadoPago;
import com.sportsync.backend.model.admin.EstadoReserva;
import com.sportsync.backend.model.cancha.Cancha;
import com.sportsync.backend.model.entidades.Rol;
import com.sportsync.backend.model.entidades.Usuario;
import com.sportsync.backend.model.reserva.Reserva;
import com.sportsync.backend.model.sede.Sede;
import com.sportsync.backend.repository.CanchaRepository;
import com.sportsync.backend.repository.PagoRepository;
import com.sportsync.backend.repository.ReservaRepository;
import com.sportsync.backend.repository.SedeRepository;
import com.sportsync.backend.repository.UsuarioRepository;
import com.sportsync.backend.service.ExpiracionReservasService;
import com.sportsync.backend.service.ReservaService;
import com.sportsync.backend.testsupport.BaseDeDatosTemporal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * T-02: una reserva PENDIENTE_PAGO bloquea el turno, vence pasado el TTL (15 min por defecto) y libera el cupo,
 * y un pago aprobado tarde se revalida contra el turno. Base temporal con el esquema completo y la migración 10.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExpiracionReservasTest {

    private static final BaseDeDatosTemporal BD = BaseDeDatosTemporal.crear("t02exp");
    private static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final String TARJETA_OK = "4111111111111111";

    @DynamicPropertySource
    static void baseTemporal(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", BD::url);
        r.add("spring.datasource.username", BD::usuario);
        r.add("spring.datasource.password", BD::clave);
    }

    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwtUtil;
    @Autowired JdbcTemplate jdbc;
    @Autowired SedeRepository sedeRepo;
    @Autowired CanchaRepository canchaRepo;
    @Autowired UsuarioRepository usuarioRepo;
    @Autowired ReservaRepository reservaRepo;
    @Autowired PagoRepository pagoRepo;
    @Autowired ReservaService reservaService;
    @Autowired ExpiracionReservasService expiracion;

    private Long canchaId;
    private final List<Usuario> usuarios = new ArrayList<>();

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

        for (int i = 1; i <= 3; i++) {
            Usuario u = new Usuario();
            u.setNombre("Usuario " + i);
            u.setEmail("exp" + i + "@prueba.com");
            u.setPassword("no-importa");
            u.setDni("2000000" + i);
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

    private MvcResult iniciarPago(Usuario u, String fecha, String inicio, String fin) throws Exception {
        String body = "{\"reserva\":{\"usuarioId\":" + u.getId() + ",\"canchaId\":" + canchaId
                + ",\"fecha\":\"" + fecha + "\",\"horaInicio\":\"" + inicio + ":00\",\"horaFin\":\"" + fin + ":00\""
                + ",\"iluminacion\":false},\"metodo\":\"TARJETA_CREDITO\"}";
        return mvc.perform(post("/pagos/iniciar")
                .header("Authorization", "Bearer " + jwtUtil.generarToken(u.getEmail(), u.getRol().name()))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
    }

    private MvcResult pagarConTarjeta(Usuario u, Long reservaId) throws Exception {
        String body = "{\"titular\":\"Test\",\"numero\":\"" + TARJETA_OK + "\",\"cvv\":\"123\","
                + "\"vencimiento\":\"12/45\",\"tipo\":\"TARJETA_CREDITO\"}";
        return mvc.perform(post("/pagos/tarjeta?reservaId=" + reservaId)
                .header("Authorization", "Bearer " + jwtUtil.generarToken(u.getEmail(), u.getRol().name()))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
    }

    /** Crea una reserva pendiente por HTTP y devuelve su id. */
    private Long reservaPendiente(Usuario u, String fecha, String inicio, String fin) throws Exception {
        MvcResult r = iniciarPago(u, fecha, inicio, fin);
        assertEquals(200, r.getResponse().getStatus(), r.getResponse().getContentAsString());
        String json = r.getResponse().getContentAsString();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"reservaId\":(\\d+)").matcher(json);
        assertTrue(m.find(), json);
        return Long.valueOf(m.group(1));
    }

    /** Simula que el pago se creó hace {@code minutos}: la antigüedad la define pago.created_at. */
    private void envejecerPago(Long reservaId, int minutos) {
        jdbc.update("update pago set created_at = now() - make_interval(mins => ?) where reserva_id = ?",
                minutos, reservaId);
    }

    private EstadoReserva estadoReserva(Long id) {
        return reservaRepo.findById(id).orElseThrow().getEstado();
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void una_reserva_pendiente_bloquea_el_turno_y_aparece_como_ocupada() throws Exception {
        String dia = fecha(2);
        Long id = reservaPendiente(usuarios.get(0), dia, "18:00", "19:00");

        assertTrue(reservaRepo.existeSolapamiento(canchaId, LocalDate.parse(dia),
                java.time.LocalTime.of(18, 0), java.time.LocalTime.of(19, 0)));
        assertEquals(409, iniciarPago(usuarios.get(1), dia, "18:00", "19:00").getResponse().getStatus());
        assertTrue(reservaService.obtenerReservasDelDia(canchaId, LocalDate.parse(dia)).stream()
                        .anyMatch(r -> r.getId().equals(id)),
                "la disponibilidad debe mostrar ocupado un turno con pago en curso");
    }

    @Test
    void una_reserva_pendiente_reciente_no_se_libera() throws Exception {
        Long id = reservaPendiente(usuarios.get(0), fecha(3), "18:00", "19:00");
        envejecerPago(id, 5);   // menos que el TTL de 15 minutos

        expiracion.expirarVencidas();

        assertEquals(EstadoReserva.PENDIENTE_PAGO, estadoReserva(id));
    }

    @Test
    void el_job_cancela_la_reserva_vencida_marca_el_pago_y_libera_el_turno() throws Exception {
        String dia = fecha(4);
        Long id = reservaPendiente(usuarios.get(0), dia, "18:00", "19:00");
        envejecerPago(id, 20);

        assertTrue(expiracion.expirarVencidas() >= 1);

        assertEquals(EstadoReserva.CANCELADA, estadoReserva(id));
        assertEquals(EstadoPago.VENCIDO, pagoRepo.findByReservaId(id).orElseThrow().getEstado());
        assertEquals(200, iniciarPago(usuarios.get(1), dia, "18:00", "19:00").getResponse().getStatus());
    }

    @Test
    void crear_una_reserva_libera_antes_los_turnos_vencidos_sin_esperar_al_job() throws Exception {
        String dia = fecha(5);
        Long id = reservaPendiente(usuarios.get(0), dia, "18:00", "19:00");
        envejecerPago(id, 20);

        // No se invoca el job: el propio pedido nuevo debe poder tomar el turno.
        assertEquals(200, iniciarPago(usuarios.get(1), dia, "18:00", "19:00").getResponse().getStatus());
        assertEquals(EstadoReserva.CANCELADA, estadoReserva(id));
    }

    @Test
    void una_reserva_confirmada_o_con_pago_aprobado_no_se_vence() throws Exception {
        Long id = reservaPendiente(usuarios.get(0), fecha(6), "18:00", "19:00");
        assertEquals(200, pagarConTarjeta(usuarios.get(0), id).getResponse().getStatus());
        envejecerPago(id, 60);

        expiracion.expirarVencidas();

        assertEquals(EstadoReserva.ACTIVA, estadoReserva(id));
    }

    @Test
    void un_pago_aprobado_tarde_reactiva_la_reserva_si_el_turno_sigue_libre() throws Exception {
        Long id = reservaPendiente(usuarios.get(0), fecha(7), "18:00", "19:00");
        envejecerPago(id, 20);
        expiracion.expirarVencidas();
        assertEquals(EstadoReserva.CANCELADA, estadoReserva(id));

        MvcResult r = pagarConTarjeta(usuarios.get(0), id);

        assertEquals(200, r.getResponse().getStatus(), r.getResponse().getContentAsString());
        assertEquals(EstadoReserva.ACTIVA, estadoReserva(id));
    }

    @Test
    void si_el_turno_vencido_ya_lo_tomo_otro_no_se_cobra_y_responde_409() throws Exception {
        String dia = fecha(1);
        Long vencida = reservaPendiente(usuarios.get(0), dia, "18:00", "19:00");
        envejecerPago(vencida, 20);
        assertEquals(200, iniciarPago(usuarios.get(1), dia, "18:00", "19:00").getResponse().getStatus());

        MvcResult r = pagarConTarjeta(usuarios.get(0), vencida);

        assertEquals(409, r.getResponse().getStatus(), r.getResponse().getContentAsString());
        assertEquals(EstadoReserva.CANCELADA, estadoReserva(vencida));
        assertEquals(EstadoPago.VENCIDO, pagoRepo.findByReservaId(vencida).orElseThrow().getEstado(),
                "el pago no debe haberse aprobado");
    }

    @Test
    void confirmar_una_reserva_vencida_cuyo_turno_fue_tomado_devuelve_false_y_no_la_reactiva() throws Exception {
        String dia = fecha(1);
        Long vencida = reservaPendiente(usuarios.get(0), dia, "20:00", "21:00");
        envejecerPago(vencida, 20);
        assertEquals(200, iniciarPago(usuarios.get(1), dia, "20:00", "21:00").getResponse().getStatus());

        Reserva r = reservaRepo.findById(vencida).orElseThrow();
        assertFalse(reservaService.confirmarReserva(r));
        assertEquals(EstadoReserva.CANCELADA, estadoReserva(vencida));
    }
}
