package com.sportsync.backend.service;

import com.sportsync.backend.dto.ReporteMensualDTO;
import com.sportsync.backend.dto.ReservaReporteDTO;
import com.sportsync.backend.dto.TotalesReporteDTO;
import com.sportsync.backend.model.admin.EstadoPago;
import com.sportsync.backend.model.admin.EstadoReserva;
import com.sportsync.backend.model.reserva.Pago;
import com.sportsync.backend.model.reserva.Reserva;
import com.sportsync.backend.repository.PagoRepository;
import com.sportsync.backend.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Arma el reporte mensual de reservas a partir de los valores YA persistidos
 * (no recalcula precios). El "cobrado" cuenta solo reservas con pago APROBADO.
 */
@Service
public class ReporteService {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final ReservaRepository reservaRepo;
    private final PagoRepository pagoRepo;

    public ReporteService(ReservaRepository reservaRepo, PagoRepository pagoRepo) {
        this.reservaRepo = reservaRepo;
        this.pagoRepo = pagoRepo;
    }

    @Transactional(readOnly = true)
    public ReporteMensualDTO generar(int mes, int anio) {
        if (mes < 1 || mes > 12) throw new IllegalArgumentException("El mes debe estar entre 1 y 12.");
        if (anio < 2000 || anio > 2100) throw new IllegalArgumentException("Año inválido.");

        YearMonth ym = YearMonth.of(anio, mes);
        LocalDate desde = ym.atDay(1);
        LocalDate hasta = ym.atEndOfMonth();

        List<Reserva> reservas = reservaRepo.findParaReporte(desde, hasta);

        // Pagos en una sola query (sin N+1), mapeados por id de reserva
        Map<Long, Pago> pagoPorReserva = reservas.isEmpty() ? Map.of()
                : pagoRepo.findByReservaIdIn(reservas.stream().map(Reserva::getId).toList())
                    .stream().collect(Collectors.toMap(p -> p.getReserva().getId(), p -> p, (a, b) -> a));

        List<ReservaReporteDTO> filas = reservas.stream()
                .map(r -> toFila(r, pagoPorReserva.get(r.getId())))
                .toList();

        TotalesReporteDTO totales = calcularTotales(reservas, pagoPorReserva);
        return new ReporteMensualDTO(mes, anio, filas, totales);
    }

    private ReservaReporteDTO toFila(Reserva r, Pago pago) {
        double descuento = redondear(r.getPrecioBase() * r.getDescuentoAplicado());
        double extras = redondear(Math.max(0, r.getPrecioTotal() - (r.getPrecioBase() - descuento)));
        double horas = Duration.between(r.getHoraInicio(), r.getHoraFin()).toMinutes() / 60.0;

        return new ReservaReporteDTO(
                r.getId(),
                r.getUsuario().getNombre(),
                r.getUsuario().getEmail(),
                r.getCancha().getSede().getNombre(),
                r.getCancha().getNombre(),
                r.getFecha().toString(),
                r.getHoraInicio().format(HM),
                r.getHoraFin().format(HM),
                redondear(horas),
                redondear(r.getPrecioBase()),
                descuento,
                extras,
                redondear(r.getPrecioTotal()),
                r.getEstado().name(),
                pago != null ? pago.getEstado().name() : "—",
                pago != null ? pago.getMetodo().name() : "—"
        );
    }

    private TotalesReporteDTO calcularTotales(List<Reserva> reservas, Map<Long, Pago> pagoPorReserva) {
        long confirmadas = reservas.stream().filter(r -> r.getEstado() == EstadoReserva.ACTIVA).count();
        long canceladas = reservas.stream().filter(r ->
                r.getEstado() == EstadoReserva.CANCELADA || r.getEstado() == EstadoReserva.RECHAZADA).count();
        long pendientes = reservas.stream().filter(r -> r.getEstado() == EstadoReserva.PENDIENTE_PAGO).count();

        double brutos = 0, descuentos = 0, extras = 0, cobrados = 0;
        for (Reserva r : reservas) {
            double descMonto = r.getPrecioBase() * r.getDescuentoAplicado();
            double ex = Math.max(0, r.getPrecioTotal() - (r.getPrecioBase() - descMonto));
            brutos += r.getPrecioTotal();
            descuentos += descMonto;
            extras += ex;
            Pago pago = pagoPorReserva.get(r.getId());
            if (pago != null && pago.getEstado() == EstadoPago.APROBADO) {
                cobrados += r.getPrecioTotal();
            }
        }
        return new TotalesReporteDTO(reservas.size(), confirmadas, canceladas, pendientes,
                redondear(brutos), redondear(descuentos), redondear(extras), redondear(cobrados));
    }

    private double redondear(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
