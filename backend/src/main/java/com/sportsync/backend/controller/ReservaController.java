package com.sportsync.backend.controller;

import com.sportsync.backend.dto.CrearReservaRequest;
import com.sportsync.backend.model.Reserva;
import com.sportsync.backend.service.ReservaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService service;

    public ReservaController(ReservaService service) {
        this.service = service;
    }

    // ── UC-30: Slots ocupados de una cancha en una fecha ──────────────────────
    // GET /reservas/disponibilidad?canchaId=1&fecha=2025-06-01

    @GetMapping("/disponibilidad")
    public ResponseEntity<?> disponibilidad(
            @RequestParam Long canchaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(service.obtenerReservasDelDia(canchaId, fecha));
    }

    // ── UC-40: Crear reserva ──────────────────────────────────────────────────
    // POST /reservas
    // Body: CrearReservaRequest

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody CrearReservaRequest req) {
        try {
            return ResponseEntity.ok(service.crearReserva(req));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── UC-41: Historial de reservas del usuario ──────────────────────────────
    // GET /reservas/usuario/{usuarioId}

    @GetMapping("/usuario/{usuarioId}")
    public List<Reserva> historial(@PathVariable Long usuarioId) {
        return service.historialUsuario(usuarioId);
    }

    // ── UC-42: Cancelar reserva ───────────────────────────────────────────────
    // PUT /reservas/{id}/cancelar
    // Body: { usuarioId }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable Long id,
                                      @RequestBody Map<String, Long> body) {
        try {
            Reserva cancelada = service.cancelarReserva(id, body.get("usuarioId"));
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Reserva cancelada correctamente.",
                    "estado",  cancelada.getEstado()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}