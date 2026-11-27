package com.sportsync.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-05: guarda contra la vuelta de secretos y credenciales a la configuración principal.
 * Lee el application.properties REAL (el de src/main, sin perfiles).
 */
class ConfiguracionSeguraTest {

    private static String principal() throws IOException {
        return new ClassPathResource("application.properties").getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void el_secreto_jwt_no_tiene_valor_por_defecto() throws IOException {
        String props = principal();
        assertTrue(props.contains("sportsync.jwt.secret=${JWT_SECRET}"),
                "sportsync.jwt.secret debe ser exactamente ${JWT_SECRET}, sin valor por defecto");
        assertFalse(props.contains("sportsync-clave-secreta"), "no debe quedar el secreto histórico");
    }

    @Test
    void las_credenciales_de_la_base_vienen_del_entorno() throws IOException {
        String props = principal();
        assertTrue(props.contains("spring.datasource.url=${DB_URL}"));
        assertTrue(props.contains("spring.datasource.username=${DB_USERNAME}"));
        assertTrue(props.contains("spring.datasource.password=${DB_PASSWORD}"));
    }

    @Test
    void el_log_del_codigo_de_verificacion_esta_apagado_por_defecto() throws IOException {
        assertTrue(principal().contains("sportsync.verificacion.modo-dev-log=${VERIF_DEV_LOG:false}"));
    }
}
