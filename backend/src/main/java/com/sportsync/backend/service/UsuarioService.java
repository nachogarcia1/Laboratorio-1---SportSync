package com.sportsync.backend.service;

import com.sportsync.backend.exception.UsuarioNoEncontradoException;
import com.sportsync.backend.model.Rol;
import com.sportsync.backend.model.Usuario;
import com.sportsync.backend.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository repo;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Listar (solo admin) ───────────────────────────────────────────────────

    public List<Usuario> listar() {
        return repo.findAll();
    }

    // ── Registro ──────────────────────────────────────────────────────────────

    public Usuario registrar(Usuario usuario) {
        if (repo.existsByEmail(usuario.getEmail())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email.");
        }
        if (repo.existsByDni(usuario.getDni())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese DNI.");
        }

        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setRol(Rol.NO_SOCIO);
        usuario.setActivo(true);

        return repo.save(usuario);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    public Usuario login(String email, String password) {
        Usuario usuario = repo.findByEmail(email)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Email o contraseña incorrectos."));

        if (!usuario.isActivo()) {
            throw new IllegalStateException("Usuario inhabilitado.");
        }

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new IllegalArgumentException("Email o contraseña incorrectos.");
        }

        return usuario;
    }

    // ── Ver perfil ────────────────────────────────────────────────────────────

    public Usuario obtenerPorEmail(String email) {
        return repo.findByEmail(email)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));
    }

    public Usuario obtenerPorId(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));
    }

    // ── Editar perfil ─────────────────────────────────────────────────────────

    public Usuario editarPerfil(Long id, String nuevoNombre, String nuevoEmail, String nuevoDni, String nuevoTelefono, String nuevaPassword) {
        Usuario usuario = obtenerPorId(id);

        if (nuevoNombre != null && !nuevoNombre.isBlank()) {
            usuario.setNombre(nuevoNombre);
        }
        if (nuevoEmail != null && !nuevoEmail.isBlank()) {
            usuario.setEmail(nuevoEmail);
        }
        if (nuevoDni != null && !nuevoDni.isBlank()) {
            usuario.setDni(nuevoDni);
        }
        if (nuevoTelefono != null && !nuevoTelefono.isBlank()) {
            usuario.setTelefono(nuevoTelefono);
        }
        if (nuevaPassword != null && !nuevaPassword.isBlank()) {
            usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        }

        return repo.save(usuario);
    }

    // ── Acreditación: NO_SOCIO → SOCIO ────────────────────────────────────────

    public Usuario acreditarSocio(Long id) {
        Usuario usuario = obtenerPorId(id);

        if (usuario.getRol() == Rol.ADMIN) {
            throw new IllegalStateException("Los administradores no pueden acreditarse como socios.");
        }

        usuario.setRol(Rol.SOCIO);
        return repo.save(usuario);
    }

    // ── Eliminar (solo admin) ─────────────────────────────────────────────────

    public void eliminarUsuario(Long id) {
        if (!repo.existsById(id)) {
            throw new UsuarioNoEncontradoException("Usuario no encontrado.");
        }
        repo.deleteById(id);
    }

    // ── Banear / desbanear (solo admin) ───────────────────────────────────────

    public Usuario toggleActivo(Long id) {
        Usuario usuario = obtenerPorId(id);
        usuario.setActivo(!usuario.isActivo());
        return repo.save(usuario);
    }
}