package com.sportsync.backend.controller;

import com.sportsync.backend.model.Cancha;
import com.sportsync.backend.model.EstadoCancha;
import com.sportsync.backend.service.CanchaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/canchas")
public class CanchaController {

    private final CanchaService service;

    public CanchaController(CanchaService service) {
        this.service = service;
    }

    // ── UC-21: Filtrar canchas (público) ──────────────────────────────────────
    // GET /canchas?sedeId=1&tipo=5

    @GetMapping
    public List<Cancha> filtrar(
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) Integer tipo) {
        return service.filtrar(sedeId, tipo);
    }

    // ── UC-20: Canchas por sede (público) ─────────────────────────────────────
    // GET /canchas/sede/{sedeId}

    @GetMapping("/sede/{sedeId}")
    public List<Cancha> listarPorSede(@PathVariable Long sedeId) {
        return service.listarHabilitadasPorSede(sedeId);
    }

    // ── Detalle de cancha (público) ───────────────────────────────────────────
    // GET /canchas/{id}

    @GetMapping("/{id}")
    public ResponseEntity<?> detalle(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.obtenerPorId(id));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // GET /canchas/admin/sede/{sedeId}
    @GetMapping("/admin/sede/{sedeId}")
    public List<Cancha> listarTodasPorSede(@PathVariable Long sedeId) {
        return service.listarPorSede(sedeId);
    }

    // ── UC-11 Admin: crear cancha en una sede ─────────────────────────────────
    // POST /canchas/sede/{sedeId}
    // Body: { nombre, tipo, precioBase }

    @PostMapping("/sede/{sedeId}")
    public ResponseEntity<?> crear(@PathVariable Long sedeId,
                                   @RequestBody Cancha cancha) {
        try {
            return ResponseEntity.ok(service.crear(sedeId, cancha));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── UC-11 Admin: editar cancha ────────────────────────────────────────────
    // PUT /canchas/{id}
    // Body: { nombre (opcional), tipo (opcional), precioBase (opcional) }

    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Long id,
                                    @RequestBody Map<String, Object> body) {
        try {
            String nombre     = (String) body.get("nombre");
            Integer tipo      = body.get("tipo") != null ? ((Number) body.get("tipo")).intValue() : null;
            Double precioBase = body.get("precioBase") != null ? ((Number) body.get("precioBase")).doubleValue() : null;

            return ResponseEntity.ok(service.editar(id, nombre, tipo, precioBase));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── UC-11 Admin: cambiar estado ───────────────────────────────────────────
    // PUT /canchas/{id}/estado
    // Body: { estado: "HABILITADA" | "MANTENIMIENTO" }

    @PutMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id,
                                           @RequestBody Map<String, String> body) {
        try {
            EstadoCancha nuevoEstado = EstadoCancha.valueOf(body.get("estado"));
            Cancha cancha = service.cambiarEstado(id, nuevoEstado);
            return ResponseEntity.ok(Map.of(
                    "estado",  cancha.getEstado(),
                    "mensaje", "Estado actualizado correctamente."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Estado inválido. Usá HABILITADA o MANTENIMIENTO."));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // ── UC-11 Admin: eliminar cancha ──────────────────────────────────────────
    // DELETE /canchas/{id}

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            service.eliminar(id);
            return ResponseEntity.ok(Map.of("mensaje", "Cancha eliminada."));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}