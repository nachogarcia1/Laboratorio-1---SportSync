package com.sportsync.backend.service;

import com.sportsync.backend.dto.CrearReservaRequest;
import com.sportsync.backend.model.admin.EstadoPago;
import com.sportsync.backend.model.admin.MetodoPago;
import com.sportsync.backend.model.reserva.Pago;
import com.sportsync.backend.model.reserva.Reserva;
import com.sportsync.backend.repository.PagoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** T-05: la simulación de pagos solo existe en el perfil dev. */
class PagoSimulacionTest {

    private PagoRepository pagoRepo = mock(PagoRepository.class);
    private ReservaService reservaService = mock(ReservaService.class);

    private PagoService servicio(String perfil, String mpToken, boolean simulacionForzada) {
        MockEnvironment env = new MockEnvironment();
        if (perfil != null) env.setActiveProfiles(perfil);
        PagoService s = new PagoService(pagoRepo, reservaService, mock(PasarelaPago.class), env);
        ReflectionTestUtils.setField(s, "accessToken", mpToken);
        ReflectionTestUtils.setField(s, "simulacionForzada", simulacionForzada);
        ReflectionTestUtils.setField(s, "frontendUrl", "http://localhost:5173");
        return s;
    }

    private void prepararReservaYPago() {
        when(reservaService.crearReservaPendiente(any())).thenReturn(new Reserva());
        when(pagoRepo.save(any(Pago.class))).thenAnswer(i -> i.getArgument(0));
        when(pagoRepo.findByReservaId(1L)).thenReturn(Optional.of(new Pago()));
    }

    @Test
    void fuera_de_dev_simular_resultado_esta_deshabilitado_aunque_falte_mp() {
        PagoService s = servicio(null, "", false);
        assertThrows(IllegalStateException.class, () -> s.simularResultado(1L, EstadoPago.APROBADO));
        verify(pagoRepo, never()).save(any());
    }

    @Test
    void fuera_de_dev_simular_resultado_esta_deshabilitado_aunque_este_forzado() {
        PagoService s = servicio("prod", "", true);
        assertThrows(IllegalStateException.class, () -> s.simularResultado(1L, EstadoPago.APROBADO));
        verify(pagoRepo, never()).save(any());
    }

    @Test
    void fuera_de_dev_sin_mp_no_se_puede_iniciar_pago_con_mercado_pago() {
        prepararReservaYPago();
        PagoService s = servicio(null, "", false);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> s.iniciarPago(new CrearReservaRequest(), MetodoPago.MERCADO_PAGO));
        assertTrue(e.getMessage().contains("MP_ACCESS_TOKEN"), e.getMessage());
    }

    @Test
    void en_dev_sin_mp_simular_resultado_funciona() {
        prepararReservaYPago();
        PagoService s = servicio("dev", "", false);
        Pago pago = s.simularResultado(1L, EstadoPago.RECHAZADO);
        assertEquals(EstadoPago.RECHAZADO, pago.getEstado());
    }

    @Test
    void en_dev_sin_mp_iniciar_pago_devuelve_el_checkout_simulado() {
        prepararReservaYPago();
        PagoService s = servicio("dev", "", false);
        PagoService.IniciarResult r = s.iniciarPago(new CrearReservaRequest(), MetodoPago.MERCADO_PAGO);
        assertEquals("SIMULADO", r.preferenciaId());
    }

    @Test
    void en_dev_con_mp_configurado_y_sin_forzar_no_simula() {
        PagoService s = servicio("dev", "TEST-token", false);
        assertThrows(IllegalStateException.class, () -> s.simularResultado(1L, EstadoPago.APROBADO));
    }
}
