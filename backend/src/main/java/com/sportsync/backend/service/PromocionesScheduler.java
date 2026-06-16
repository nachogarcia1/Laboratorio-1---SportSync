package com.sportsync.backend.service;

import com.sportsync.backend.model.entidades.Rol;
import com.sportsync.backend.model.entidades.Usuario;
import com.sportsync.backend.model.reserva.DescuentoHorario;
import com.sportsync.backend.repository.DescuentoHorarioRepository;
import com.sportsync.backend.repository.ReservaRepository;
import com.sportsync.backend.repository.UsuarioRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PromocionesScheduler {

    private final DescuentoHorarioRepository descuentoRepo;
    private final ReservaRepository reservaRepo;
    private final UsuarioRepository usuarioRepo;
    private final EmailService emailService;

    public PromocionesScheduler(DescuentoHorarioRepository descuentoRepo,
                                ReservaRepository reservaRepo,
                                UsuarioRepository usuarioRepo,
                                EmailService emailService) {
        this.descuentoRepo = descuentoRepo;
        this.reservaRepo   = reservaRepo;
        this.usuarioRepo   = usuarioRepo;
        this.emailService  = emailService;
    }

    /** Corre a las 10am todos los días y envía promociones personalizadas. */
    /*@Scheduled(cron = "0 * * * * *")*/ // cada minuto (solo para probar)
    @Scheduled(cron = "0 0 10 * * *")
    @Transactional(readOnly = true)
    public void enviarPromociones() {
        // 1. Descuentos activos con cancha y sede ya cargados (JOIN FETCH)
        List<DescuentoHorario> descuentos = descuentoRepo.findDescuentosActivosConCancha();
        if (descuentos.isEmpty()) return;

        // 2. Sedes que tienen descuento
        Set<Long> sedesConDescuento = descuentos.stream()
                .map(dh -> dh.getCancha().getSede().getId())
                .collect(Collectors.toSet());

        // 3. Usuarios no-admin que quieren promociones
        List<Usuario> usuarios = usuarioRepo.findByRolNot(Rol.ADMIN).stream()
                .filter(Usuario::isRecibirPromociones)
                .toList();

        // 4. Por cada usuario, cruzar sus sedes visitadas con las que tienen descuento
        for (Usuario usuario : usuarios) {
            Set<Long> sedesVisitadas = reservaRepo.findSedesConHistorialDeUsuario(usuario.getId());
            Set<Long> sedesRelevantes = new HashSet<>(sedesConDescuento);
            sedesRelevantes.retainAll(sedesVisitadas);
            if (sedesRelevantes.isEmpty()) continue;

            List<DescuentoHorario> relevantes = descuentos.stream()
                    .filter(dh -> sedesRelevantes.contains(dh.getCancha().getSede().getId()))
                    .collect(Collectors.toList());

            emailService.enviarPromocionesResumen(usuario.getEmail(), usuario.getNombre(), relevantes);
        }
    }
}