package com.sportsync.backend.service;

import com.sportsync.backend.model.admin.EstadoReserva;
import com.sportsync.backend.model.reserva.Reserva;
import com.sportsync.backend.repository.ReservaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class RecordatorioScheduler {

    private final ReservaRepository reservaRepo;
    private final EmailService emailService;

    public RecordatorioScheduler(ReservaRepository reservaRepo, EmailService emailService) {
        this.reservaRepo = reservaRepo;
        this.emailService = emailService;
    }

    /** Corre a las 9am todos los días y envía recordatorios de reservas del día siguiente. */
    /*@Scheduled(cron = "0 * * * * *")*/ // cada minuto (solo para probar)
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional(readOnly = true)
    public void enviarRecordatorios() {
        LocalDate manana = LocalDate.now().plusDays(1);
        List<Reserva> reservas = reservaRepo.findByFechaAndEstado(manana, EstadoReserva.ACTIVA);

        for (Reserva reserva : reservas) {
            if (!reserva.getUsuario().isRecibirRecordatorios()) continue;
            emailService.enviarRecordatorio(
                    reserva.getUsuario().getEmail(),
                    reserva.getUsuario().getNombre(),
                    "Fútbol " + reserva.getCancha().getTipo(),
                    reserva.getCancha().getSede().getNombre(),
                    reserva.getFecha().toString(),
                    reserva.getHoraInicio().toString(),
                    reserva.getHoraFin().toString()
            );
        }
    }
}