package com.sportsync.backend.controller;

import com.sportsync.backend.config.Autorizacion;
import com.sportsync.backend.model.reserva.Reserva;
import com.sportsync.backend.service.ReservaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService service;
    private final Autorizacion authz;

    public ReservaController(ReservaService service, Autorizacion authz) {
        this.service = service;
        this.authz = authz;
    }

    // ── UC-30: Slots ocupados de una cancha en una fecha ──────────────────────
    // GET /reservas/disponibilidad?canchaId=1&fecha=2025-06-01

    @GetMapping("/disponibilidad")
    public ResponseEntity<?> disponibilidad(
            @RequestParam Long canchaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(service.obtenerReservasDelDia(canchaId, fecha));
    }

    // ── Config pública de reservas (para que el front grise turnos igual que el back) ──
    // GET /reservas/config → { antelacionMinimaMinutos }
    @GetMapping("/config")
    public ResponseEntity<?> config() {
        return ResponseEntity.ok(Map.of(
                "antelacionMinimaMinutos", service.getAntelacionMinimaMinutos()));
    }

    // ── UC-41: Historial de reservas del usuario ──────────────────────────────
    // GET /reservas/usuario/{usuarioId}

    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("@authz.esUsuarioOAdmin(#usuarioId)")
    public List<Reserva> historial(@PathVariable Long usuarioId) {
        return service.historialUsuario(usuarioId);
    }

    // GET /reservas/usuario/{usuarioId}/sin-calificar-admin
    @GetMapping("/usuario/{usuarioId}/sin-calificar-admin")
    public List<Reserva> sinCalificarAdmin(@PathVariable Long usuarioId) {
        return service.reservasSinCalificarAdmin(usuarioId);
    }

    // ── UC-42: Cancelar reserva ───────────────────────────────────────────────
    // PUT /reservas/{id}/cancelar
    // Body: { usuarioId } (se ignora: el usuario sale del JWT)

    @PutMapping("/{id}/cancelar")
    @PreAuthorize("@authz.esDuenioDeReserva(#id)")
    public ResponseEntity<?> cancelar(@PathVariable Long id,
                                      @RequestBody Map<String, Long> body) {
        try {
            Reserva cancelada = service.cancelarReserva(id, authz.idActual());
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Reserva cancelada correctamente.",
                    "estado",  cancelada.getEstado()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /reservas/precio-preview?canchaId=1&hora=19:00
    @GetMapping("/precio-preview")
    public ResponseEntity<?> precioPreview(
            @RequestParam Long canchaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hora,
            @RequestParam(required = false) Long usuarioId, // se ignora: el usuario sale del JWT
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        try {
            return ResponseEntity.ok(service.calcularPrecioPreview(canchaId, hora, authz.idActual(), fecha));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /reservas/descuentos?canchaId=1
    @GetMapping("/descuentos")
    public ResponseEntity<?> descuentos(@RequestParam Long canchaId) {
        return ResponseEntity.ok(service.obtenerDescuentosCancha(canchaId));
    }
}