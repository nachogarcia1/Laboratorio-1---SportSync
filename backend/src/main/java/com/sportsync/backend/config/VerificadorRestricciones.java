package com.sportsync.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Al arrancar, avisa si falta la restricción anti-solapamiento de reservas.
 *
 * Esa restricción NO la puede crear Hibernate ({@code ddl-auto=update} solo crea tablas y columnas):
 * viene de la migración {@code db/migrations/2026-11-28_10_reserva_sin_solapamiento.sql}, que hay que
 * aplicar también en una base nueva. Sin ella, dos pedidos simultáneos pueden reservar el mismo turno.
 *
 * Solo avisa (no frena el arranque) para no bloquear el desarrollo local, pero lo hace con nivel ERROR.
 */
@Component
public class VerificadorRestricciones implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(VerificadorRestricciones.class);

    static final String RESTRICCION_SOLAPAMIENTO = "reserva_sin_solapamiento";

    private final JdbcTemplate jdbc;

    public VerificadorRestricciones(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer cantidad = jdbc.queryForObject(
                    "select count(*) from pg_constraint where conname = ?", Integer.class, RESTRICCION_SOLAPAMIENTO);
            if (cantidad == null || cantidad == 0) {
                log.error("FALTA la restricción '{}' en la tabla reserva: sin ella, dos pedidos simultáneos pueden "
                        + "reservar el mismo turno. Aplicá db/migrations/2026-11-28_10_reserva_sin_solapamiento.sql "
                        + "(ver db/migrations/README.md).", RESTRICCION_SOLAPAMIENTO);
            }
        } catch (Exception e) {
            log.warn("No se pudo verificar la restricción '{}': {}", RESTRICCION_SOLAPAMIENTO, e.getMessage());
        }
    }
}
