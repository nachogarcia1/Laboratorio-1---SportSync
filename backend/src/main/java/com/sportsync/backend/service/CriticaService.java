package com.sportsync.backend.service;

import com.sportsync.backend.exception.UsuarioNoEncontradoException;
import com.sportsync.backend.model.*;
import com.sportsync.backend.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CriticaService {

    private final CriticaUsuarioRepository criticaUsuarioRepo;
    private final CriticaCanchaRepository criticaCanchaRepo;
    private final UsuarioRepository usuarioRepo;
    private final CanchaRepository canchaRepo;
    private final ReservaRepository reservaRepo;

    public CriticaService(CriticaUsuarioRepository criticaUsuarioRepo,
                          CriticaCanchaRepository criticaCanchaRepo,
                          UsuarioRepository usuarioRepo,
                          CanchaRepository canchaRepo,
                          ReservaRepository reservaRepo) {
        this.criticaUsuarioRepo = criticaUsuarioRepo;
        this.criticaCanchaRepo = criticaCanchaRepo;
        this.usuarioRepo = usuarioRepo;
        this.canchaRepo = canchaRepo;
        this.reservaRepo = reservaRepo;
    }

    // ── Rating de usuario ─────────────────────────────────────────────────────

    public double getRatingUsuario(Long usuarioId) {
        Double rating = criticaUsuarioRepo.calcularRating(usuarioId);
        return rating != null ? Math.round(rating * 10.0) / 10.0 : 0.0;
    }

    public List<CriticaUsuario> getCriticasUsuario(Long usuarioId) {
        return criticaUsuarioRepo.findByUsuarioId(usuarioId);
    }

    public CriticaUsuario criticarUsuario(Long adminId, Long usuarioId, int nota, String comentario) {
        validarNota(nota);

        Usuario admin = usuarioRepo.findById(adminId)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Admin no encontrado."));

        if (admin.getRol() != Rol.ADMIN) {
            throw new IllegalStateException("Solo los administradores pueden criticar usuarios.");
        }

        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));

        CriticaUsuario critica = new CriticaUsuario();
        critica.setAdmin(admin);
        critica.setUsuario(usuario);
        critica.setNota(nota);
        critica.setComentario(comentario);

        return criticaUsuarioRepo.save(critica);
    }

    // ── Rating de cancha ──────────────────────────────────────────────────────

    public double getRatingCancha(Long canchaId) {
        Double rating = criticaCanchaRepo.calcularRatingCancha(canchaId);
        return rating != null ? Math.round(rating * 10.0) / 10.0 : 0.0;
    }

    public double getRatingSede(Long sedeId) {
        Double rating = criticaCanchaRepo.calcularRatingSede(sedeId);
        return rating != null ? Math.round(rating * 10.0) / 10.0 : 0.0;
    }

    public List<CriticaCancha> getCriticasCancha(Long canchaId) {
        return criticaCanchaRepo.findByCanchaId(canchaId);
    }

    public CriticaCancha criticarCancha(Long usuarioId, Long canchaId,
                                        Long reservaId, int nota, String comentario) {
        validarNota(nota);

        // Verificar que la reserva existe y pertenece al usuario
        Reserva reserva = reservaRepo.findById(reservaId)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Reserva no encontrada."));

        if (!reserva.getUsuario().getId().equals(usuarioId)) {
            throw new IllegalStateException("Solo podés criticar canchas que vos reservaste.");
        }

        if (reserva.getEstado() != EstadoReserva.ACTIVA) {
            throw new IllegalStateException("Solo podés criticar reservas activas.");
        }

        // Verificar que no haya crítica previa para esta reserva
        if (criticaCanchaRepo.existsByReservaId(reservaId)) {
            throw new IllegalStateException("Ya criticaste esta reserva.");
        }

        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));

        Cancha cancha = canchaRepo.findById(canchaId)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Cancha no encontrada."));

        CriticaCancha critica = new CriticaCancha();
        critica.setUsuario(usuario);
        critica.setCancha(cancha);
        critica.setReserva(reserva);
        critica.setNota(nota);
        critica.setComentario(comentario);

        return criticaCanchaRepo.save(critica);
    }

    // ── Validación ────────────────────────────────────────────────────────────

    private void validarNota(int nota) {
        if (nota < 1 || nota > 5) {
            throw new IllegalArgumentException("La nota debe ser entre 1 y 5.");
        }
    }
}