package com.sportsync.backend.service;

import com.sportsync.backend.dto.ReporteMensualDTO;
import com.sportsync.backend.model.admin.EstadoPago;
import com.sportsync.backend.model.admin.EstadoReserva;
import com.sportsync.backend.model.admin.MetodoPago;
import com.sportsync.backend.model.cancha.Cancha;
import com.sportsync.backend.model.entidades.Usuario;
import com.sportsync.backend.model.reserva.Pago;
import com.sportsync.backend.model.reserva.Reserva;
import com.sportsync.backend.model.sede.Sede;
import com.sportsync.backend.repository.PagoRepository;
import com.sportsync.backend.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReporteServiceTest {

    private ReservaRepository reservaRepo;
    private PagoRepository pagoRepo;
    private ReporteService service;

    @BeforeEach
    void setup() {
        reservaRepo = mock(ReservaRepository.class);
        pagoRepo = mock(PagoRepository.class);
        service = new ReporteService(reservaRepo, pagoRepo);
    }

    private Reserva reserva(long id, EstadoReserva estado, double base, double desc, double total) {
        Sede sede = new Sede(); sede.setNombre("Palermo");
        Cancha cancha = new Cancha(); cancha.setNombre("Cancha 1"); cancha.setSede(sede);
        Usuario u = new Usuario(); u.setNombre("Juan"); u.setEmail("j@b.com");
        Reserva r = new Reserva();
        ReflectionTestUtils.setField(r, "id", id);
        r.setUsuario(u); r.setCancha(cancha);
        r.setFecha(LocalDate.of(2026, 6, 10));
        r.setHoraInicio(LocalTime.of(19, 0)); r.setHoraFin(LocalTime.of(20, 0));
        r.setEstado(estado); r.setPrecioBase(base); r.setDescuentoAplicado(desc); r.setPrecioTotal(total);
        return r;
    }

    private Pago pago(Reserva r, EstadoPago estado) {
        Pago p = new Pago();
        p.setReserva(r); p.setEstado(estado); p.setMetodo(MetodoPago.TARJETA_CREDITO); p.setMonto(r.getPrecioTotal());
        return p;
    }

    @Test
    void cobradoSoloCuentaPagosAprobadosYDesgloseCorrecto() {
        Reserva r1 = reserva(1, EstadoReserva.ACTIVA, 1000, 0.10, 1400); // aprobado → cobrado
        Reserva r2 = reserva(2, EstadoReserva.ACTIVA, 2000, 0.0, 2000);  // rechazado → NO cobrado
        Reserva r3 = reserva(3, EstadoReserva.CANCELADA, 500, 0.0, 500);  // cancelada, sin pago

        when(reservaRepo.findParaReporte(any(), any())).thenReturn(List.of(r1, r2, r3));
        when(pagoRepo.findByReservaIdIn(any())).thenReturn(List.of(
                pago(r1, EstadoPago.APROBADO), pago(r2, EstadoPago.RECHAZADO)));

        ReporteMensualDTO rep = service.generar(6, 2026);

        assertEquals(3, rep.totales().cantidad());
        assertEquals(2, rep.totales().confirmadas());
        assertEquals(1, rep.totales().canceladas());
        assertEquals(0, rep.totales().pendientes());
        assertEquals(3900, rep.totales().ingresosBrutos());   // 1400+2000+500
        assertEquals(100, rep.totales().descuentos());        // 1000*0.10
        assertEquals(500, rep.totales().ingresosExtras());    // r1: 1400-900=500; r2,r3: 0
        assertEquals(1400, rep.totales().ingresosCobrados());  // SOLO r1 (APROBADO)
    }

    @Test
    void reservaSinPagoMuestraGuionYNoSeCobra() {
        Reserva r = reserva(10, EstadoReserva.PENDIENTE_PAGO, 1000, 0.0, 1000);
        when(reservaRepo.findParaReporte(any(), any())).thenReturn(List.of(r));
        when(pagoRepo.findByReservaIdIn(any())).thenReturn(List.of()); // sin pago

        ReporteMensualDTO rep = service.generar(6, 2026);

        assertEquals("—", rep.reservas().get(0).estadoPago());
        assertEquals(0, rep.totales().ingresosCobrados());
        assertEquals(1, rep.totales().pendientes());
    }

    @Test
    void mesInvalidoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> service.generar(13, 2026));
    }
}
