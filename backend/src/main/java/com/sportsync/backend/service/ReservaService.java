package com.sportsync.backend.service;

import com.sportsync.backend.dto.CrearReservaRequest;
import com.sportsync.backend.exception.UsuarioNoEncontradoException;
import com.sportsync.backend.model.*;
import com.sportsync.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepo;
    private final CanchaRepository canchaRepo;
    private final UsuarioRepository usuarioRepo;
    private final ItemEquipamientoRepository itemRepo;
    private final ReservaEquipamientoRepository reservaEquipRepo;

    // Horas mínimas para cancelar sin multa (configurable)
    private static final int HORAS_MIN_CANCELACION = 2;
    // Precio fijo de iluminación
    private static final double PRECIO_ILUMINACION = 500.0;

    public ReservaService(ReservaRepository reservaRepo,
                          CanchaRepository canchaRepo,
                          UsuarioRepository usuarioRepo,
                          ItemEquipamientoRepository itemRepo,
                          ReservaEquipamientoRepository reservaEquipRepo) {
        this.reservaRepo = reservaRepo;
        this.canchaRepo = canchaRepo;
        this.usuarioRepo = usuarioRepo;
        this.itemRepo = itemRepo;
        this.reservaEquipRepo = reservaEquipRepo;
    }

    // ── UC-30: Slots disponibles ──────────────────────────────────────────────

    public List<Reserva> obtenerReservasDelDia(Long canchaId, LocalDate fecha) {
        return reservaRepo.findByCanchaIdAndFechaAndEstado(canchaId, fecha, EstadoReserva.ACTIVA);
    }

    // ── UC-40: Crear reserva ──────────────────────────────────────────────────

    @Transactional
    public Reserva crearReserva(CrearReservaRequest req) {
        // Validar que la cancha existe y está habilitada
        Cancha cancha = canchaRepo.findById(req.getCanchaId())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Cancha no encontrada."));

        if (cancha.getEstado() != EstadoCancha.HABILITADA) {
            throw new IllegalStateException("La cancha está en mantenimiento.");
        }

        // Validar que la fecha no sea en el pasado
        if (req.getFecha().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("No podés reservar en una fecha pasada.");
        }

        // UC-31: Validar anti-solapamiento
        boolean solapado = reservaRepo.existeSolapamiento(
                req.getCanchaId(), req.getFecha(), req.getHoraInicio(), req.getHoraFin());

        if (solapado) {
            throw new IllegalStateException("El horario solicitado ya está reservado.");
        }

        Usuario usuario = usuarioRepo.findById(req.getUsuarioId())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));

        // Calcular precio base
        double precio = cancha.getPrecioBase();
        if (req.isIluminacion()) precio += PRECIO_ILUMINACION;

        // Crear reserva
        Reserva reserva = new Reserva();
        reserva.setCancha(cancha);
        reserva.setUsuario(usuario);
        reserva.setFecha(req.getFecha());
        reserva.setHoraInicio(req.getHoraInicio());
        reserva.setHoraFin(req.getHoraFin());
        reserva.setIluminacion(req.isIluminacion());
        reserva.setEstado(EstadoReserva.ACTIVA);

        // Agregar equipamiento si hay
        if (req.getEquipamiento() != null && !req.getEquipamiento().isEmpty()) {
            for (CrearReservaRequest.ItemCantidad ic : req.getEquipamiento()) {
                ItemEquipamiento item = itemRepo.findById(ic.getItemId())
                        .orElseThrow(() -> new UsuarioNoEncontradoException("Item no encontrado."));
                precio += item.getPrecioPorUnidad() * ic.getCantidad();
            }
        }

        reserva.setPrecioTotal(precio);
        Reserva guardada = reservaRepo.save(reserva);

        // Guardar detalle de equipamiento
        if (req.getEquipamiento() != null) {
            for (CrearReservaRequest.ItemCantidad ic : req.getEquipamiento()) {
                ItemEquipamiento item = itemRepo.findById(ic.getItemId()).get();
                ReservaEquipamiento re = new ReservaEquipamiento();
                re.setReserva(guardada);
                re.setItem(item);
                re.setCantidad(ic.getCantidad());
                re.setPrecioUnitario(item.getPrecioPorUnidad());
                reservaEquipRepo.save(re);
            }
        }

        return guardada;
    }

    // ── UC-41: Historial de reservas ──────────────────────────────────────────

    public List<Reserva> historialUsuario(Long usuarioId) {
        return reservaRepo.findByUsuarioId(usuarioId);
    }

    // ── UC-42: Cancelar reserva ───────────────────────────────────────────────

    @Transactional
    public Reserva cancelarReserva(Long reservaId, Long usuarioId) {
        Reserva reserva = reservaRepo.findById(reservaId)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Reserva no encontrada."));

        if (!reserva.getUsuario().getId().equals(usuarioId)) {
            throw new IllegalStateException("No podés cancelar una reserva que no es tuya.");
        }

        if (reserva.getEstado() != EstadoReserva.ACTIVA) {
            throw new IllegalStateException("La reserva ya está cancelada o rechazada.");
        }

        // Verificar regla de horas mínimas
        LocalDateTime inicioReserva = LocalDateTime.of(reserva.getFecha(), reserva.getHoraInicio());
        LocalDateTime ahora = LocalDateTime.now();
        long horasRestantes = java.time.Duration.between(ahora, inicioReserva).toHours();

        if (horasRestantes < HORAS_MIN_CANCELACION) {
            throw new IllegalStateException(
                    "No podés cancelar con menos de " + HORAS_MIN_CANCELACION + " horas de anticipación.");
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        return reservaRepo.save(reserva);
    }
}