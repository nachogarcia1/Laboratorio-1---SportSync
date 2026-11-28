package com.sportsync.backend.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** T-05: el secreto JWT es obligatorio y mínimo de 32 bytes; sin él la app no puede arrancar. */
class JwtUtilTest {

    private static final String SECRETO_VALIDO = "un-secreto-de-prueba-con-mas-de-32-caracteres";

    @Test
    void sin_secreto_falla() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil(null));
        assertThrows(IllegalStateException.class, () -> new JwtUtil(""));
        assertThrows(IllegalStateException.class, () -> new JwtUtil("   "));
    }

    @Test
    void secreto_corto_falla_y_dice_cuanto_mide() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> new JwtUtil("corto"));
        assertTrue(e.getMessage().contains("5 bytes"), e.getMessage());
    }

    @Test
    void secreto_valido_genera_y_valida_tokens() {
        JwtUtil jwt = new JwtUtil(SECRETO_VALIDO);
        String token = jwt.generarToken("a@b.com", "ADMIN");
        assertTrue(jwt.esValido(token));
        assertEquals("a@b.com", jwt.extraerEmail(token));
        assertEquals("ADMIN", jwt.extraerRol(token));
    }

    @Test
    void token_firmado_con_otro_secreto_no_es_valido() {
        String token = new JwtUtil(SECRETO_VALIDO).generarToken("a@b.com", "ADMIN");
        JwtUtil otro = new JwtUtil("otro-secreto-distinto-con-mas-de-32-caracteres");
        assertFalse(otro.esValido(token));
    }

    /** El secreto que estaba escrito en el código ya es público (historial de git): no debe servir. */
    @Test
    void el_secreto_historico_del_repo_no_valida_tokens_nuevos() {
        JwtUtil historico = new JwtUtil("sportsync-clave-secreta-muy-larga-para-hmac-256");
        String tokenDelHistorico = historico.generarToken("atacante@x.com", "ADMIN");
        assertFalse(new JwtUtil(SECRETO_VALIDO).esValido(tokenDelHistorico));
    }
}
