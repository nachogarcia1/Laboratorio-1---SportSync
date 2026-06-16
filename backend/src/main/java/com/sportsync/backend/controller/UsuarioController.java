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
    private final com.sportsync.backend.service.VerificacionService verificacionService;
    private final com.sportsync.backend.service.GoogleTokenVerifier googleTokenVerifier;
    private final com.sportsync.backend.config.JwtUtil jwtUtil;

    public UsuarioController(UsuarioService service,
                             com.sportsync.backend.service.VerificacionService verificacionService,
                             com.sportsync.backend.service.GoogleTokenVerifier googleTokenVerifier,
                             com.sportsync.backend.config.JwtUtil jwtUtil) {
        this.service = service;
        this.verificacionService = verificacionService;
        this.googleTokenVerifier = googleTokenVerifier;
        this.jwtUtil = jwtUtil;
    }

    private Map<String, Object> sesionDe(Usuario usuario) {
        return Map.of(
                "token",  jwtUtil.generarToken(usuario.getEmail(), usuario.getRol().name()),
                "id",     usuario.getId(),
                "nombre", usuario.getNombre(),
                "email",  usuario.getEmail(),
                "rol",    usuario.getRol()
        );
    }

    @PostMapping("/register")
    public ResponseEntity<?> registrar(@RequestBody Usuario usuario) {
        try {
            Usuario creado = service.registrar(usuario);
            verificacionService.generarYEnviar(creado);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Te enviamos un código de verificación a tu email.",
                    "email",   creado.getEmail(),
                    "requiereVerificacion", true
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            Usuario usuario = service.login(body.get("email"), body.get("password"));
            return ResponseEntity.ok(sesionDe(usuario));
        } catch (com.sportsync.backend.exception.CuentaNoVerificadaException e) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", e.getMessage(),
                    "requiereVerificacion", true,
                    "email", body.get("email")
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verificar")
    public ResponseEntity<?> verificar(@RequestBody Map<String, String> body) {
        try {
            Usuario usuario = verificacionService.verificar(body.get("email"), body.get("codigo"));
            return ResponseEntity.ok(sesionDe(usuario));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reenviar-codigo")
    public ResponseEntity<?> reenviarCodigo(@RequestBody Map<String, String> body) {
        try {
            verificacionService.reenviar(body.get("email"));
            return ResponseEntity.ok(Map.of("mensaje", "Te reenviamos el código a tu email."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/oauth/google")
    public ResponseEntity<?> oauthGoogle(@RequestBody Map<String, String> body) {
        try {
            var g = googleTokenVerifier.verificar(body.get("idToken"));
            if (!g.emailVerified())
                return ResponseEntity.badRequest().body(Map.of("error", "Tu email de Google no está verificado."));
            Usuario usuario = service.loginConGoogle(g.email(), g.nombre());
            return ResponseEntity.ok(sesionDe(usuario));
        } catch (IllegalArgumentException | IllegalStateException e) {
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
    public ResponseEntity<?> acreditar(@PathVariable Long id,
                                       @RequestBody com.sportsync.backend.dto.DatosTarjeta tarjeta) {
        try {
            Membresia membresia = service.acreditarSocio(id, tarjeta);
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

    @PutMapping("/{id}/preferencias")
    public ResponseEntity<?> actualizarPreferencias(@PathVariable Long id,
                                                    @RequestBody Map<String, Boolean> body) {
        try {
            boolean recordatorios = Boolean.TRUE.equals(body.get("recibirRecordatorios"));
            boolean promociones   = Boolean.TRUE.equals(body.get("recibirPromociones"));
            service.actualizarPreferencias(id, recordatorios, promociones);
            return ResponseEntity.ok(Map.of(
                    "recibirRecordatorios", recordatorios,
                    "recibirPromociones",   promociones
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