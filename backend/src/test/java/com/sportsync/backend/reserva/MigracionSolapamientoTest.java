package com.sportsync.backend.reserva;

import com.sportsync.backend.testsupport.BaseDeDatosTemporal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-01: prueba la migración 10 (restricción anti-solapamiento) directamente contra PostgreSQL,
 * en una base temporal y con una tabla {@code reserva} mínima. No levanta Spring.
 */
class MigracionSolapamientoTest {

    private static final Path MIGRACION = BaseDeDatosTemporal.migracion("2026-11-28_10_reserva_sin_solapamiento.sql");
    private static final Path ROLLBACK = BaseDeDatosTemporal.migracion("2026-11-28_10_reserva_sin_solapamiento_rollback.sql");
    private static final String EXCLUSION_VIOLATION = "23P01";

    private static BaseDeDatosTemporal bd;

    @BeforeAll
    static void crearBase() {
        bd = BaseDeDatosTemporal.crear("t01mig");
    }

    @AfterAll
    static void eliminarBase() {
        bd.eliminar();
    }

    @BeforeEach
    void tablaLimpia() throws Exception {
        bd.ejecutar("DROP TABLE IF EXISTS reserva");
        bd.ejecutar("CREATE TABLE reserva (id bigserial PRIMARY KEY, cancha_id bigint NOT NULL, fecha date NOT NULL, "
                + "hora_inicio time(0) NOT NULL, hora_fin time(0) NOT NULL, estado varchar(20) NOT NULL)");
    }

    private void insertar(long cancha, String fecha, String inicio, String fin, String estado) throws SQLException {
        bd.ejecutar("INSERT INTO reserva (cancha_id, fecha, hora_inicio, hora_fin, estado) VALUES ("
                + cancha + ", '" + fecha + "', '" + inicio + "', '" + fin + "', '" + estado + "')");
    }

    private boolean existeRestriccion() throws SQLException {
        try (Connection c = bd.conectar(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM pg_constraint WHERE conname = 'reserva_sin_solapamiento'")) {
            rs.next();
            return rs.getInt(1) == 1;
        }
    }

    private void aplicar() throws Exception {
        bd.ejecutarArchivo(MIGRACION);
    }

    private static void assertViolacion(org.junit.jupiter.api.function.Executable e) {
        SQLException ex = assertThrows(SQLException.class, e);
        assertEquals(EXCLUSION_VIOLATION, ex.getSQLState(), ex.getMessage());
    }

    @Test
    void se_aplica_sobre_una_tabla_sin_solapamientos_y_es_idempotente() throws Exception {
        insertar(1, "2026-12-01", "18:00", "19:00", "ACTIVA");
        insertar(1, "2026-12-01", "19:00", "20:00", "PENDIENTE_PAGO");
        aplicar();
        aplicar(); // volver a aplicarla no rompe nada
        assertTrue(existeRestriccion());
    }

    @Test
    void aborta_sin_cambiar_nada_si_ya_hay_reservas_solapadas() throws Exception {
        insertar(1, "2026-12-01", "18:00", "19:00", "ACTIVA");
        insertar(1, "2026-12-01", "18:30", "19:30", "PENDIENTE_PAGO");

        SQLException e = assertThrows(SQLException.class, this::aplicar);
        assertTrue(e.getMessage().contains("Abortado"), e.getMessage());
        assertFalse(existeRestriccion(), "la restricción no debe quedar creada si la guarda abortó");

        // Cancelando la duplicada ya se puede aplicar.
        bd.ejecutar("UPDATE reserva SET estado = 'CANCELADA' WHERE hora_inicio = '18:30'");
        aplicar();
        assertTrue(existeRestriccion());
    }

    @Test
    void rechaza_el_mismo_turno_y_los_solapamientos_parciales() throws Exception {
        aplicar();
        insertar(1, "2026-12-01", "18:00", "19:00", "ACTIVA");
        assertViolacion(() -> insertar(1, "2026-12-01", "18:00", "19:00", "PENDIENTE_PAGO"));
        assertViolacion(() -> insertar(1, "2026-12-01", "18:30", "19:30", "ACTIVA"));
        assertViolacion(() -> insertar(1, "2026-12-01", "17:30", "18:30", "ACTIVA"));
        assertViolacion(() -> insertar(1, "2026-12-01", "17:00", "20:00", "ACTIVA")); // lo contiene
    }

    @Test
    void permite_turnos_contiguos_otra_cancha_otra_fecha_y_reservas_canceladas() throws Exception {
        aplicar();
        insertar(1, "2026-12-01", "18:00", "19:00", "ACTIVA");
        insertar(1, "2026-12-01", "19:00", "20:00", "ACTIVA");      // contiguo: el rango es [inicio, fin)
        insertar(1, "2026-12-01", "17:00", "18:00", "ACTIVA");      // contiguo por delante
        insertar(2, "2026-12-01", "18:00", "19:00", "ACTIVA");      // otra cancha
        insertar(1, "2026-12-02", "18:00", "19:00", "ACTIVA");      // otro día
        insertar(1, "2026-12-01", "18:00", "19:00", "CANCELADA");   // las canceladas no cuentan
        insertar(1, "2026-12-01", "18:00", "19:00", "RECHAZADA");   // las rechazadas tampoco
    }

    @Test
    void cancelar_una_reserva_libera_el_turno() throws Exception {
        aplicar();
        insertar(1, "2026-12-01", "18:00", "19:00", "PENDIENTE_PAGO");
        assertViolacion(() -> insertar(1, "2026-12-01", "18:00", "19:00", "ACTIVA"));

        bd.ejecutar("UPDATE reserva SET estado = 'CANCELADA' WHERE hora_inicio = '18:00'");
        insertar(1, "2026-12-01", "18:00", "19:00", "ACTIVA");
    }

    @Test
    void pasar_de_pendiente_a_activa_no_choca_consigo_misma() throws Exception {
        aplicar();
        insertar(1, "2026-12-01", "18:00", "19:00", "PENDIENTE_PAGO");
        bd.ejecutar("UPDATE reserva SET estado = 'ACTIVA' WHERE hora_inicio = '18:00'");
    }

    @Test
    void un_turno_que_termina_a_medianoche_se_interpreta_hasta_el_dia_siguiente() throws Exception {
        aplicar();
        insertar(1, "2026-12-01", "23:00", "00:00", "ACTIVA");
        assertViolacion(() -> insertar(1, "2026-12-01", "23:30", "00:30", "ACTIVA"));
        insertar(1, "2026-12-02", "00:00", "01:00", "ACTIVA"); // empieza justo cuando termina el anterior
    }

    @Test
    void el_rollback_elimina_la_restriccion() throws Exception {
        aplicar();
        assertTrue(existeRestriccion());
        bd.ejecutarArchivo(ROLLBACK);
        assertFalse(existeRestriccion());
        insertar(1, "2026-12-01", "18:00", "19:00", "ACTIVA");
        insertar(1, "2026-12-01", "18:00", "19:00", "ACTIVA"); // ya no hay protección
    }
}
