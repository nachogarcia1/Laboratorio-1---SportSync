package com.sportsync.backend.service;

import com.sportsync.backend.dto.DatosTarjeta;
import com.sportsync.backend.model.admin.MetodoPago;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/** Tests del validador de tarjeta, con Clock fijo (hoy = 2026-06-15) para el vencimiento. */
class ValidadorTarjetaTest {

    private final Clock clockFijo = Clock.fixed(
            LocalDate.of(2026, 6, 15).atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
    private final ValidadorTarjeta validador = new ValidadorTarjeta(clockFijo);

    private DatosTarjeta tarjeta(String titular, String numero, String cvv, String venc) {
        DatosTarjeta t = new DatosTarjeta();
        t.setTitular(titular); t.setNumero(numero); t.setCvv(cvv);
        t.setVencimiento(venc); t.setTipo(MetodoPago.TARJETA_CREDITO);
        return t;
    }

    @Test
    void tarjetaValida() {
        assertTrue(validador.validar(tarjeta("Juan Perez", "4111111111111111", "123", "12/30")).isEmpty());
    }

    @Test
    void numeroConEspaciosYGuionesSeNormaliza() {
        assertTrue(validador.validar(tarjeta("Juan Perez", "4111-1111 1111-1111", "123", "12/30")).isEmpty());
    }

    @Test
    void numeroInvalidoSegunLuhn() {
        assertTrue(validador.validar(tarjeta("Juan Perez", "4111111111111112", "123", "12/30")).isPresent());
    }

    @Test
    void numeroConCaracteresNoNumericos() {
        assertTrue(validador.validar(tarjeta("Juan Perez", "4111abcd11111111", "123", "12/30")).isPresent());
    }

    @Test
    void numeroDemasiadoCorto() {
        assertTrue(validador.validar(tarjeta("Juan Perez", "411111", "123", "12/30")).isPresent());
    }

    @Test
    void cvvInvalido() {
        assertTrue(validador.validar(tarjeta("Juan Perez", "4111111111111111", "12", "12/30")).isPresent());
        assertTrue(validador.validar(tarjeta("Juan Perez", "4111111111111111", "12a", "12/30")).isPresent());
    }

    @Test
    void mesDeExpiracionInvalido() {
        assertTrue(validador.validar(tarjeta("Juan Perez", "4111111111111111", "123", "13/30")).isPresent());
    }

    @Test
    void tarjetaVencida() {
        // 01/2026 ya pasó respecto a 06/2026
        assertTrue(validador.validar(tarjeta("Juan Perez", "4111111111111111", "123", "01/26")).isPresent());
    }

    @Test
    void tarjetaValidaDuranteElMesDeVencimiento() {
        // 06/2026 == mes actual → debe ser válida (vale hasta fin de mes)
        assertTrue(validador.validar(tarjeta("Juan Perez", "4111111111111111", "123", "06/26")).isEmpty());
    }

    @Test
    void titularVacio() {
        assertTrue(validador.validar(tarjeta("   ", "4111111111111111", "123", "12/30")).isPresent());
    }

    @Test
    void titularSoloNumerosEsInvalido() {
        assertTrue(validador.validar(tarjeta("12345", "4111111111111111", "123", "12/30")).isPresent());
    }
}
