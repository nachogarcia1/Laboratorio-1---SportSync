package com.sportsync.backend.controller;

import com.sportsync.backend.model.Usuario;
import com.sportsync.backend.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // ── UC-01: Registrarse ────────────────────────────────────────────────────
    // POST /usuarios/register
    // Body: { nombre, email, password, dni, telefono (opcional) }

    @PostMapping("/register")
    public ResponseEntity<?> registrar(@RequestBody Usuario usuario) {
        try {
            Usuario creado = service.registrar(usuario);
            return ResponseEntity.ok(creado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── UC-01: Login ──────────────────────────────────────────────────────────
    // POST /usuarios/login
    // Body: { email, password }

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


    // ── UC-03: Ver perfil ─────────────────────────────────────────────────────
    // GET /usuarios/{id}

    @GetMapping("/{id}")
    public ResponseEntity<?> verPerfil(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.obtenerPorId(id));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // ── UC-03: Editar perfil ──────────────────────────────────────────────────
    // PUT /usuarios/{id}
    // Body: { nombre (opcional), password (opcional) }

    @PutMapping("/{id}")
    public ResponseEntity<?> editarPerfil(@PathVariable Long id,
                                          @RequestBody Map<String, String> body) {
        try {
            Usuario actualizado = service.editarPerfil(
                    id,
                    body.get("nombre"),
                    body.get("password")
            );
            return ResponseEntity.ok(actualizado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── UC-03: Acreditarse como socio ─────────────────────────────────────────
    // PUT /usuarios/{id}/acreditar

    @PutMapping("/{id}/acreditar")
    public ResponseEntity<?> acreditar(@PathVariable Long id) {
        try {
            Usuario actualizado = service.acreditarSocio(id);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Acreditación exitosa.",
                    "rol",     actualizado.getRol()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Admin: listar todos los usuarios ─────────────────────────────────────
    // GET /usuarios

    @GetMapping
    public List<Usuario> listar() {
        return service.listar();
    }

    // ── Admin: banear / desbanear usuario ─────────────────────────────────────
    // PUT /usuarios/{id}/toggle-activo

    @PutMapping("/{id}/toggle-activo")
    public ResponseEntity<?> toggleActivo(@PathVariable Long id) {
        try {
            Usuario usuario = service.toggleActivo(id);
            return ResponseEntity.ok(Map.of(
                    "activo",  usuario.isActivo(),
                    "mensaje", usuario.isActivo() ? "Usuario habilitado." : "Usuario baneado."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Admin: eliminar usuario ───────────────────────────────────────────────
    // DELETE /usuarios/{id}

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            service.eliminarUsuario(id);
            return ResponseEntity.ok(Map.of("mensaje", "Usuario eliminado."));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}