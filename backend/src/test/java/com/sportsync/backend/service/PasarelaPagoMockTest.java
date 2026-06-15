package com.sportsync.backend.service;

import com.sportsync.backend.dto.DatosTarjeta;
import com.sportsync.backend.dto.ResultadoPago;
import com.sportsync.backend.dto.SolicitudPago;
import com.sportsync.backend.model.admin.EstadoResultadoPago;
import com.sportsync.backend.model.admin.MetodoPago;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class PasarelaPagoMockTest {

    private final Clock clockFijo = Clock.fixed(
            LocalDate.of(2026, 6, 15).atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
    private final PasarelaPagoMock pasarela = new PasarelaPagoMock(new ValidadorTarjeta(clockFijo));

    private SolicitudPago solicitud(String numero) {
        DatosTarjeta t = new DatosTarjeta();
        t.setTitular("Juan Perez"); t.setNumero(numero); t.setCvv("123");
        t.setVencimiento("12/30"); t.setTipo(MetodoPago.TARJETA_CREDITO);
        return new SolicitudPago(BigDecimal.valueOf(1000), "Test", t);
    }

    @Test
    void pagoAprobado() {
        ResultadoPago r = pasarela.cobrar(solicitud("4111 1111 1111 1111"));
        assertEquals(EstadoResultadoPago.APROBADO, r.getEstado());
        assertEquals("1111", r.getUltimos4());
        assertNotNull(r.getTransaccionId());
    }

    @Test
    void pagoRechazadoConTarjetaMagica() {
        ResultadoPago r = pasarela.cobrar(solicitud("4000 0000 0000 0002"));
        assertEquals(EstadoResultadoPago.RECHAZADO, r.getEstado());
        assertEquals("0002", r.getUltimos4());
        assertNull(r.getTransaccionId());
    }

    @Test
    void datosInvalidosNoLanzaExcepcion() {
        ResultadoPago r = pasarela.cobrar(solicitud("4111111111111112")); // falla Luhn
        assertEquals(EstadoResultadoPago.DATOS_INVALIDOS, r.getEstado());
        assertNull(r.getTransaccionId());
    }
}
