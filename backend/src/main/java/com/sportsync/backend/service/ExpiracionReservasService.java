package com.sportsync.backend.service;

import com.sportsync.backend.model.admin.EstadoPago;
import com.sportsync.backend.model.admin.EstadoReserva;
import com.sportsync.backend.model.reserva.Pago;
import com.sportsync.backend.repository.PagoRepository;
import com.sportsync.backend.repository.ReservaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Libera los turnos bloqueados por reservas PENDIENTE_PAGO que nunca se pagaron.
 * Una reserva pendiente ocupa el turno (ver la restricción de la migración 10); sin este job,
 * un pago abandonado lo bloquearía para siempre.
 */
@Service
public class ExpiracionReservasService {

    private static final Logger log = LoggerFactory.getLogger(ExpiracionReservasService.class);

    private final PagoRepository pagoRepo;
    private final ReservaRepository reservaRepo;
    private final Clock clock;

    @Value("${sportsync.reservas.pendiente-ttl-minutos:15}")
    private long ttlMinutos;

    public ExpiracionReservasService(PagoRepository pagoRepo, ReservaRepository reservaRepo, Clock clock) {
        this.pagoRepo = pagoRepo;
        this.reservaRepo = reservaRepo;
        this.clock = clock;
    }

    /** Job periódico. El intervalo se configura con sportsync.reservas.expiracion-intervalo-ms. */
    @Scheduled(fixedDelayString = "${sportsync.reservas.expiracion-intervalo-ms:60000}")
    public void expirarPorJob() {
        int n = expirarVencidas();
        if (n > 0) log.info("Se liberaron {} turno(s) de reservas sin pagar (TTL {} min).", n, ttlMinutos);
    }

    /**
     * Pasa a VENCIDO los pagos pendientes con más de TTL minutos y a CANCELADA su reserva.
     * Se llama también al crear una reserva, para que un turno vencido se pueda tomar sin esperar al job.
     *
     * @return cantidad de reservas liberadas
     */
    @Transactional
    public int expirarVencidas() {
        LocalDateTime limite = LocalDateTime.now(clock).minusMinutes(ttlMinutos);
        List<Pago> vencidos = pagoRepo.findPendientesCreadosAntesDe(limite);
        for (Pago pago : vencidos) {
            pago.setEstado(EstadoPago.VENCIDO);
            pagoRepo.save(pago);
            pago.getReserva().setEstado(EstadoReserva.CANCELADA);
            reservaRepo.save(pago.getReserva());
        }
        // Se fuerza el UPDATE ahora: quien nos llamó va a consultar solapamientos a continuación.
        if (!vencidos.isEmpty()) reservaRepo.flush();
        return vencidos.size();
    }
}
