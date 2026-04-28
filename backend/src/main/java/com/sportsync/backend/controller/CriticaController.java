package com.sportsync.backend.controller;

import com.sportsync.backend.dto.CriticaCanchaResponse;
import com.sportsync.backend.model.critica.CriticaCancha;
import com.sportsync.backend.model.critica.CriticaUsuario;
import com.sportsync.backend.service.CriticaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/criticas")
public class CriticaController {

    private final CriticaService service;

    public CriticaController(CriticaService service) {
        this.service = service;
    }

    // ── Usuarios ──────────────────────────────────────────────────────────────

    @GetMapping("/usuarios/{id}")
    public List<CriticaUsuario> criticasUsuario(@PathVariable Long id) {
        return service.getCriticasUsuario(id);
    }

    @GetMapping("/usuarios/{id}/rating")
    public ResponseEntity<?> ratingUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("rating", service.getRatingUsuario(id)));
    }

    @PostMapping("/usuarios")
    public ResponseEntity<?> criticarUsuario(@RequestBody Map<String, Object> body) {
        try {
            Long adminId      = ((Number) body.get("adminId")).longValue();
            Long usuarioId    = ((Number) body.get("usuarioId")).longValue();
            int nota          = ((Number) body.get("nota")).intValue();
            String comentario = (String) body.get("comentario");
            return ResponseEntity.ok(service.criticarUsuario(adminId, usuarioId, nota, comentario));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Canchas ───────────────────────────────────────────────────────────────

    @GetMapping("/canchas/{id}")
    public List<CriticaCancha> criticasCancha(@PathVariable Long id) {
        return service.getCriticasCancha(id);
    }

    @GetMapping("/canchas/{id}/rating")
    public ResponseEntity<?> ratingCancha(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("rating", service.getRatingCancha(id)));
    }

    @GetMapping("/canchas/usuario/{usuarioId}")
    public List<CriticaCanchaResponse> criticasPorUsuario(@PathVariable Long usuarioId) {
        return service.getCriticasByUsuario(usuarioId);
    }

    @GetMapping("/canchas/reservas/{reservaId}")
    public ResponseEntity<?> existeCritica(@PathVariable Long reservaId) {
        return ResponseEntity.ok(Map.of("yaCalificada", service.yaCalificoReserva(reservaId)));
    }

    @GetMapping("/sedes/{id}/rating")
    public ResponseEntity<?> ratingSede(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("rating", service.getRatingSede(id)));
    }

    @PostMapping("/canchas")
    public ResponseEntity<?> criticarCancha(@RequestBody Map<String, Object> body) {
        try {
            Long usuarioId    = ((Number) body.get("usuarioId")).longValue();
            Long canchaId     = ((Number) body.get("canchaId")).longValue();
            Long reservaId    = ((Number) body.get("reservaId")).longValue();
            int notaCancha    = ((Number) body.get("notaCancha")).intValue();
            int notaStaff     = ((Number) body.get("notaStaff")).intValue();
            int notaServicios = ((Number) body.get("notaServicios")).intValue();
            String comentario = (String) body.get("comentario");
            return ResponseEntity.ok(
                    service.criticarCancha(usuarioId, canchaId, reservaId,
                            notaCancha, notaStaff, notaServicios, comentario));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}