package com.sportsync.backend.service;

import com.sportsync.backend.dto.CrearReservaRequest;
import com.sportsync.backend.exception.UsuarioNoEncontradoException;
import com.sportsync.backend.model.admin.EstadoCancha;
import com.sportsync.backend.model.admin.EstadoReserva;
import com.sportsync.backend.model.cancha.ItemEquipamiento;
import com.sportsync.backend.model.cancha.Cancha;
import com.sportsync.backend.model.entidades.Usuario;
import com.sportsync.backend.model.reserva.Reserva;
import com.sportsync.backend.model.reserva.ReservaEquipamiento;
import com.sportsync.backend.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.sportsync.backend.model.entidades.Rol;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepo;
    private final CanchaRepository canchaRepo;
    private final UsuarioRepository usuarioRepo;
    private final ItemEquipamientoRepository itemRepo;
    private final ReservaEquipamientoRepository reservaEquipRepo;
    private final PrecioInteligenteService precioService;

    // Horas mínimas para cancelar sin multa (configurable)
    private static final int HORAS_MIN_CANCELACION = 2;
    // Precio fijo de iluminación
    private static final double PRECIO_ILUMINACION = 500.0;

    private static final double DESCUENTO_SOCIO       = 0.10;
    private static final int    MAX_RESERVAS_SOCIO     = 5;
    private static final int    MAX_RESERVAS_NO_SOCIO  = 2;
    private static final int    DIAS_ANTICIPO_SOCIO    = 30;
    private static final int    DIAS_ANTICIPO_NO_SOCIO = 7;

    // Antelación mínima (minutos) para reservar un turno; en la tz de la sede
    @Value("${sportsync.reservas.antelacion-minima-minutos:30}")
    private int antelacionMinimaMinutos;

    public ReservaService(ReservaRepository reservaRepo,
                          CanchaRepository canchaRepo,
                          UsuarioRepository usuarioRepo,
                          ItemEquipamientoRepository itemRepo,
                          ReservaEquipamientoRepository reservaEquipRepo, PrecioInteligenteService precioService) {
        this.reservaRepo = reservaRepo;
        this.canchaRepo = canchaRepo;
        this.usuarioRepo = usuarioRepo;
        this.itemRepo = itemRepo;
        this.reservaEquipRepo = reservaEquipRepo;
        this.precioService = precioService;
    }

    // ── UC-30: Slots disponibles ──────────────────────────────────────────────

    public List<Reserva> obtenerReservasDelDia(Long canchaId, LocalDate fecha) {
        return reservaRepo.findByCanchaIdAndFechaAndEstado(canchaId, fecha, EstadoReserva.ACTIVA);
    }

    /** Antelación mínima (minutos) para reservar; el frontend la usa para grisar turnos. */
    public int getAntelacionMinimaMinutos() {
        return antelacionMinimaMinutos;
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

        // Validar horario vencido / antelación mínima, usando la zona horaria de la sede.
        // Esta validación es la autoridad: aunque el frontend lo permita, el backend rechaza.
        ZoneId zona = ZoneId.of(cancha.getSede().getZonaHoraria());
        ZonedDateTime ahora = ZonedDateTime.now(zona);
        ZonedDateTime inicioReserva = ZonedDateTime.of(req.getFecha(), req.getHoraInicio(), zona);

        if (inicioReserva.isBefore(ahora)) {
            throw new IllegalArgumentException("No podés reservar en un horario que ya pasó.");
        }
        if (inicioReserva.isBefore(ahora.plusMinutes(antelacionMinimaMinutos))) {
            throw new IllegalArgumentException(
                    "Tenés que reservar con al menos " + antelacionMinimaMinutos + " minutos de anticipación.");
        }

        // UC-31: Validar anti-solapamiento
        boolean solapado = reservaRepo.existeSolapamiento(
                req.getCanchaId(), req.getFecha(), req.getHoraInicio(), req.getHoraFin());

        if (solapado) {
            throw new IllegalStateException("El horario solicitado ya está reservado.");
        }

        Usuario usuario = usuarioRepo.findById(req.getUsuarioId())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));

        boolean esSocio = usuario.getRol() == Rol.SOCIO;

        // Validar días máximos de anticipación
        int diasMaximos = esSocio ? DIAS_ANTICIPO_SOCIO : DIAS_ANTICIPO_NO_SOCIO;
        if (req.getFecha().isAfter(LocalDate.now().plusDays(diasMaximos))) {
            throw new IllegalArgumentException(
                    "Solo podés reservar con hasta " + diasMaximos + " días de anticipación." +
                            (!esSocio ? " Los socios pueden reservar hasta " + DIAS_ANTICIPO_SOCIO + " días antes." : "")
            );
        }

        // Validar máximo de reservas activas
        int maxReservas = esSocio ? MAX_RESERVAS_SOCIO : MAX_RESERVAS_NO_SOCIO;
        long reservasActivas = reservaRepo.countByUsuarioIdAndEstado(req.getUsuarioId(), EstadoReserva.ACTIVA);
        if (reservasActivas >= maxReservas) {
            throw new IllegalStateException(
                    "Alcanzaste el límite de " + maxReservas + " reservas activas." +
                            (!esSocio ? " Los socios pueden tener hasta " + MAX_RESERVAS_SOCIO + "." : "")
            );
        }

        // Iluminación: solo disponible si el turno arranca a las 18:00 o más tarde (validación dura)
        if (req.isIluminacion() && req.getHoraInicio().isBefore(LocalTime.of(18, 0))) {
            throw new IllegalArgumentException("La iluminación solo está disponible para turnos desde las 18:00.");
        }

        // Calcular precio base
        double descuento = precioService.obtenerDescuento(req.getCanchaId(), req.getHoraInicio());
        double precio    = cancha.getPrecioBase() * (1 - descuento);
        if (esSocio) precio *= (1 - DESCUENTO_SOCIO);
        if (req.isIluminacion()) precio += PRECIO_ILUMINACION * (esSocio ? (1 - DESCUENTO_SOCIO) : 1.0);

        // Crear reserva
        Reserva reserva = new Reserva();
        reserva.setCancha(cancha);
        reserva.setUsuario(usuario);
        reserva.setFecha(req.getFecha());
        reserva.setHoraInicio(req.getHoraInicio());
        reserva.setHoraFin(req.getHoraFin());
        reserva.setIluminacion(req.isIluminacion());
        reserva.setEstado(EstadoReserva.ACTIVA);
        reserva.setPrecioBase(cancha.getPrecioBase());
        reserva.setDescuentoAplicado(descuento);

        // Agregar equipamiento si hay (validando disponibilidad y stock)
        if (req.getEquipamiento() != null && !req.getEquipamiento().isEmpty()) {
            for (CrearReservaRequest.ItemCantidad ic : req.getEquipamiento()) {
                ItemEquipamiento item = itemRepo.findById(ic.getItemId())
                        .orElseThrow(() -> new UsuarioNoEncontradoException("Item no encontrado."));
                if (!item.isDisponible()) {
                    throw new IllegalStateException("El extra '" + item.getNombre() + "' no está disponible.");
                }
                if (ic.getCantidad() <= 0) {
                    throw new IllegalArgumentException("La cantidad de '" + item.getNombre() + "' debe ser mayor a 0.");
                }
                if (ic.getCantidad() > item.getStock()) {
                    throw new IllegalStateException("No hay stock suficiente de '" + item.getNombre()
                            + "' (disponible: " + item.getStock() + ").");
                }
                double precioItem = item.getPrecioPorUnidad() * ic.getCantidad();
                if (esSocio) precioItem *= (1 - DESCUENTO_SOCIO);
                precio += precioItem;
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
        Long canchaIdTrigger = req.getCanchaId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                precioService.recalcularCancha(canchaIdTrigger);
            }
        });

        return guardada;
    }

    // ── UC-41: Historial de reservas ──────────────────────────────────────────

    public List<Reserva> historialUsuario(Long usuarioId) {
        return reservaRepo.findByUsuarioId(usuarioId);
    }

    public List<Reserva> reservasSinCalificarAdmin(Long usuarioId) {
        return reservaRepo.findReservasSinCalificarAdmin(usuarioId, LocalDate.now(), LocalTime.now());
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

    public Map<String, Object> calcularPrecioPreview(Long canchaId, LocalTime hora, Long usuarioId) {
        Cancha cancha = canchaRepo.findById(canchaId)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Cancha no encontrada."));

        boolean esSocio = false;
        if (usuarioId != null) {
            Usuario u = usuarioRepo.findById(usuarioId).orElse(null);
            esSocio = u != null && u.getRol() == Rol.SOCIO;
        }

        double descuento                   = precioService.obtenerDescuento(canchaId, hora);
        double precioTrasPrecioInteligente = cancha.getPrecioBase() * (1 - descuento);
        double precioFinal                 = esSocio ? precioTrasPrecioInteligente * (1 - DESCUENTO_SOCIO) : precioTrasPrecioInteligente;

        Map<String, Object> result = new HashMap<>();
        result.put("precioBase",                  cancha.getPrecioBase());
        result.put("descuentoPorcentaje",         Math.round(descuento * 100));
        result.put("precioTrasPrecioInteligente", precioTrasPrecioInteligente);
        result.put("precioFinal",                 precioFinal);
        result.put("esSocio",                     esSocio);
        result.put("descuentoSocio",              esSocio ? 10 : 0);
        return result;
    }

    public List<Map<String, Object>> obtenerDescuentosCancha(Long canchaId) {
        return precioService.obtenerDescuentosPorCancha(canchaId);
    }
}