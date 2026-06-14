package com.sportsync.backend.controller;

import com.sportsync.backend.model.cancha.ItemEquipamiento;
import com.sportsync.backend.service.ItemEquipamientoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/equipamiento")
public class ItemEquipamientoController {

    private final ItemEquipamientoService service;

    public ItemEquipamientoController(ItemEquipamientoService service) {
        this.service = service;
    }

    // GET /equipamiento — listado público.
    // Con ?sedeId=X devuelve los extras de esa sede + los globales; sin param, todos los disponibles.
    @GetMapping
    public List<ItemEquipamiento> listar(@RequestParam(required = false) Long sedeId) {
        return sedeId != null ? service.listarParaSede(sedeId) : service.listarDisponibles();
    }

    // GET /equipamiento/admin/todos — admin
    @GetMapping("/admin/todos")
    public List<ItemEquipamiento> listarTodos() {
        return service.listarTodos();
    }

    // POST /equipamiento — admin
    // Body: { nombre, precio, stock, disponible, sedeId? }  (sedeId null = ítem global)
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body) {
        try {
            String nombre   = (String) body.get("nombre");
            double precio   = body.get("precio") != null ? ((Number) body.get("precio")).doubleValue() : 0;
            int stock       = body.get("stock") != null ? ((Number) body.get("stock")).intValue() : 0;
            boolean activo  = body.get("disponible") == null || Boolean.TRUE.equals(body.get("disponible"));
            Long sedeId     = body.get("sedeId") != null ? ((Number) body.get("sedeId")).longValue() : null;
            return ResponseEntity.ok(service.crear(nombre, precio, stock, activo, sedeId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /equipamiento/{id} — admin
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Long id,
                                    @RequestBody Map<String, Object> body) {
        try {
            String nombre  = (String) body.get("nombre");
            Double precio  = body.get("precio") != null ? ((Number) body.get("precio")).doubleValue() : null;
            Integer stock  = body.get("stock")  != null ? ((Number) body.get("stock")).intValue()    : null;
            return ResponseEntity.ok(service.editar(id, nombre, precio, stock));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /equipamiento/{id}/toggle — admin
    @PutMapping("/{id}/toggle")
    public ResponseEntity<?> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(service.toggleDisponible(id));
    }

    // DELETE /equipamiento/{id} — admin
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.ok(Map.of("mensaje", "Item eliminado."));
    }
}