package com.sportsync.backend.controller;

import com.sportsync.backend.model.CriticaCancha;
import com.sportsync.backend.model.CriticaUsuario;
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

    // GET /criticas/usuarios/{id} — ver críticas de un usuario
    @GetMapping("/usuarios/{id}")
    public List<CriticaUsuario> criticasUsuario(@PathVariable Long id) {
        return service.getCriticasUsuario(id);
    }

    // GET /criticas/usuarios/{id}/rating — ver rating de un usuario
    @GetMapping("/usuarios/{id}/rating")
    public ResponseEntity<?> ratingUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("rating", service.getRatingUsuario(id)));
    }

    // POST /criticas/usuarios — admin critica a un usuario
    // Body: { adminId, usuarioId, nota, comentario }
    @PostMapping("/usuarios")
    public ResponseEntity<?> criticarUsuario(@RequestBody Map<String, Object> body) {
        try {
            Long adminId    = ((Number) body.get("adminId")).longValue();
            Long usuarioId  = ((Number) body.get("usuarioId")).longValue();
            int nota        = ((Number) body.get("nota")).intValue();
            String comentario = (String) body.get("comentario");

            return ResponseEntity.ok(service.criticarUsuario(adminId, usuarioId, nota, comentario));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Canchas ───────────────────────────────────────────────────────────────

    // GET /criticas/canchas/{id} — ver críticas de una cancha
    @GetMapping("/canchas/{id}")
    public List<CriticaCancha> criticasCancha(@PathVariable Long id) {
        return service.getCriticasCancha(id);
    }

    // GET /criticas/canchas/{id}/rating — ver rating de una cancha
    @GetMapping("/canchas/{id}/rating")
    public ResponseEntity<?> ratingCancha(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("rating", service.getRatingCancha(id)));
    }

    // GET /criticas/sedes/{id}/rating — ver rating de una sede
    @GetMapping("/sedes/{id}/rating")
    public ResponseEntity<?> ratingSede(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("rating", service.getRatingSede(id)));
    }

    // POST /criticas/canchas — usuario critica una cancha
    // Body: { usuarioId, canchaId, reservaId, nota, comentario }
    @PostMapping("/canchas")
    public ResponseEntity<?> criticarCancha(@RequestBody Map<String, Object> body) {
        try {
            Long usuarioId  = ((Number) body.get("usuarioId")).longValue();
            Long canchaId   = ((Number) body.get("canchaId")).longValue();
            Long reservaId  = ((Number) body.get("reservaId")).longValue();
            int nota        = ((Number) body.get("nota")).intValue();
            String comentario = (String) body.get("comentario");

            return ResponseEntity.ok(
                    service.criticarCancha(usuarioId, canchaId, reservaId, nota, comentario));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}