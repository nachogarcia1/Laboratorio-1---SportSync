package com.sportsync.backend.controller;

import com.sportsync.backend.model.Sede;
import com.sportsync.backend.service.SedeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sedes")
public class SedeController {

    private final SedeService service;

    public SedeController(SedeService service) {
        this.service = service;
    }

    // ── UC-20: Listado público (solo activas) ─────────────────────────────────
    // GET /sedes

    @GetMapping
    public List<Sede> listarActivas() {
        return service.listarActivas();
    }

    // ── UC-20: Detalle de sede ────────────────────────────────────────────────
    // GET /sedes/{id}

    @GetMapping("/{id}")
    public ResponseEntity<?> detalle(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.obtenerPorId(id));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // ── UC-10 Admin: listado completo ─────────────────────────────────────────
    // GET /sedes/admin/todas

    @GetMapping("/admin/todas")
    public List<Sede> listarTodas() {
        return service.listarTodas();
    }

    // ── UC-10 Admin: crear sede ───────────────────────────────────────────────
    // POST /sedes
    // Body: { nombre, direccion, horarios }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Sede sede) {
        try {
            return ResponseEntity.ok(service.crear(sede));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── UC-10 Admin: editar sede ──────────────────────────────────────────────
    // PUT /sedes/{id}
    // Body: { nombre (opcional), direccion (opcional), horarios (opcional) }

    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Long id,
                                    @RequestBody Map<String, String> body) {
        try {
            Sede actualizada = service.editar(
                    id,
                    body.get("nombre"),
                    body.get("direccion"),
                    body.get("horarios")
            );
            return ResponseEntity.ok(actualizada);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── UC-10 Admin: habilitar / deshabilitar sede ────────────────────────────
    // PUT /sedes/{id}/toggle-activa

    @PutMapping("/{id}/toggle-activa")
    public ResponseEntity<?> toggleActiva(@PathVariable Long id) {
        try {
            Sede sede = service.toggleActiva(id);
            return ResponseEntity.ok(Map.of(
                    "activa",  sede.isActiva(),
                    "mensaje", sede.isActiva() ? "Sede habilitada." : "Sede deshabilitada."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // ── UC-10 Admin: eliminar sede ────────────────────────────────────────────
    // DELETE /sedes/{id}

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            service.eliminar(id);
            return ResponseEntity.ok(Map.of("mensaje", "Sede eliminada."));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}