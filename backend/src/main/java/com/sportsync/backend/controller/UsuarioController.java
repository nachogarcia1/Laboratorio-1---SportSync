package com.sportsync.backend.controller;

import com.sportsync.backend.model.entidades.Usuario;
import com.sportsync.backend.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sportsync.backend.model.admin.Membresia;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService service;
    private final com.sportsync.backend.config.JwtUtil jwtUtil;

    public UsuarioController(UsuarioService service, com.sportsync.backend.config.JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registrar(@RequestBody Usuario usuario) {
        try {
            return ResponseEntity.ok(service.registrar(usuario));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            Usuario usuario = service.login(body.get("email"), body.get("password"));
            String token = jwtUtil.generarToken(usuario.getEmail(), usuario.getRol().name());
            return ResponseEntity.ok(Map.of(
                    "token",  token,
                    "id",     usuario.getId(),
                    "nombre", usuario.getNombre(),
                    "email",  usuario.getEmail(),
                    "rol",    usuario.getRol()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/buscar")
    public ResponseEntity<?> buscar(@RequestParam String email) {
        try {
            return ResponseEntity.ok(service.obtenerPorEmail(email));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> verPerfil(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.obtenerPorId(id));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editarPerfil(@PathVariable Long id,
                                          @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(service.editarPerfil(
                    id, body.get("nombre"), body.get("email"),
                    body.get("dni"), body.get("telefono"), body.get("password")
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/acreditar")
    public ResponseEntity<?> acreditar(@PathVariable Long id) {
        try {
            Membresia membresia = service.acreditarSocio(id);
            Usuario usuario = service.obtenerPorId(id);
            String nuevoToken = jwtUtil.generarToken(usuario.getEmail(), usuario.getRol().name());
            return ResponseEntity.ok(Map.of(
                    "mensaje",               "Acreditación exitosa.",
                    "rol",                   usuario.getRol(),
                    "token",                 nuevoToken,
                    "fechaInicioSocio",      membresia.getFechaInicio().toString(),
                    "fechaVencimientoSocio", membresia.getFechaVencimiento().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public List<Usuario> listar() {
        return service.listar();
    }

    @PutMapping("/{id}/suspender")
    public ResponseEntity<?> suspender(@PathVariable Long id,
                                       @RequestBody Map<String, Object> body) {
        try {
            String tipo = (String) body.get("tipo");
            Integer dias = body.get("dias") != null ? ((Number) body.get("dias")).intValue() : null;
            Usuario usuario = service.suspender(id, tipo, dias);
            return ResponseEntity.ok(Map.of(
                    "estado", usuario.getEstado(),
                    "mensaje", "Usuario suspendido correctamente."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/rehabilitar")
    public ResponseEntity<?> rehabilitar(@PathVariable Long id) {
        try {
            service.rehabilitar(id);
            return ResponseEntity.ok(Map.of("mensaje", "Usuario rehabilitado."));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            service.eliminarUsuario(id);
            return ResponseEntity.ok(Map.of("mensaje", "Usuario eliminado."));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/cancelar-socio")
    public ResponseEntity<?> cancelarSocio(@PathVariable Long id) {
        try {
            Usuario actualizado = service.cancelarSocio(id);
            String nuevoToken = jwtUtil.generarToken(actualizado.getEmail(), actualizado.getRol().name());
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Membresía cancelada.",
                    "rol",     actualizado.getRol(),
                    "token",   nuevoToken
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}