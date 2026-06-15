package com.sportsync.backend.service;

import com.sportsync.backend.exception.CuentaNoVerificadaException;
import com.sportsync.backend.exception.UsuarioNoEncontradoException;
import com.sportsync.backend.model.admin.*;
import com.sportsync.backend.model.entidades.Rol;
import com.sportsync.backend.model.entidades.Usuario;
import com.sportsync.backend.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final SuspensionRepository suspensionRepo;
    private final MembresiaRepository membresiaRepo;
    private final PasarelaPago pasarelaPago;

    @org.springframework.beans.factory.annotation.Value("${sportsync.socio.cuota:5000}")
    private double cuotaSocio;

    public UsuarioService(UsuarioRepository repo, PasswordEncoder passwordEncoder,
                          SuspensionRepository suspensionRepo, MembresiaRepository membresiaRepo,
                          PasarelaPago pasarelaPago) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
        this.suspensionRepo = suspensionRepo;
        this.membresiaRepo = membresiaRepo;
        this.pasarelaPago = pasarelaPago;
    }

    public List<Usuario> listar() { return repo.findByRolNot(Rol.ADMIN); }

    public Usuario registrar(Usuario usuario) {
        if (repo.existsByEmail(usuario.getEmail()))
            throw new IllegalArgumentException("Ya existe un usuario con ese email.");
        if (repo.existsByDni(usuario.getDni()))
            throw new IllegalArgumentException("Ya existe un usuario con ese DNI.");
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setRol(Rol.NO_SOCIO);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        return repo.save(usuario);
    }

    public Usuario login(String email, String password) {
        Usuario usuario = repo.findByEmail(email)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Email o contraseña incorrectos."));

        if (usuario.getEstado() == EstadoUsuario.SUSPENDIDO_PERMANENTE)
            throw new IllegalStateException("Tu cuenta está suspendida permanentemente.");

        if (usuario.getEstado() == EstadoUsuario.SUSPENDIDO_TEMPORAL) {
            Optional<Suspension> suspension = suspensionRepo.findByUsuarioIdAndActivaTrue(usuario.getId());
            if (suspension.isPresent() && suspension.get().getFechaFin() != null
                    && LocalDate.now().isAfter(suspension.get().getFechaFin())) {
                suspension.get().setActiva(false);
                suspensionRepo.save(suspension.get());
                usuario.setEstado(EstadoUsuario.ACTIVO);
                repo.save(usuario);
            } else {
                String hasta = suspension.map(s -> s.getFechaFin() != null
                        ? s.getFechaFin().toString() : "indefinidamente").orElse("indefinidamente");
                throw new IllegalStateException("Tu cuenta está suspendida hasta " + hasta + ".");
            }
        }

        if (!passwordEncoder.matches(password, usuario.getPassword()))
            throw new IllegalArgumentException("Email o contraseña incorrectos.");

        if (!usuario.isVerificado())
            throw new CuentaNoVerificadaException("Verificá tu email antes de iniciar sesión.");

        return usuario;
    }

    /**
     * Login/alta vía Google OAuth. Si el email ya existe, lo loguea (marcándolo
     * verificado, porque Google validó el email). Si no, crea la cuenta con
     * authProvider=GOOGLE, sin DNI y con una contraseña aleatoria no usable.
     */
    public Usuario loginConGoogle(String email, String nombre) {
        return repo.findByEmail(email)
                .map(usuario -> {
                    if (usuario.getEstado() == EstadoUsuario.SUSPENDIDO_PERMANENTE)
                        throw new IllegalStateException("Tu cuenta está suspendida permanentemente.");
                    if (!usuario.isVerificado()) {
                        usuario.setVerificado(true);
                        repo.save(usuario);
                    }
                    return usuario;
                })
                .orElseGet(() -> {
                    Usuario nuevo = new Usuario();
                    nuevo.setNombre(nombre != null && !nombre.isBlank() ? nombre : email);
                    nuevo.setEmail(email);
                    nuevo.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
                    nuevo.setDni(null); // se completa luego desde el perfil
                    nuevo.setTelefono(null);
                    nuevo.setRol(Rol.NO_SOCIO);
                    nuevo.setEstado(EstadoUsuario.ACTIVO);
                    nuevo.setVerificado(true);
                    nuevo.setAuthProvider(com.sportsync.backend.model.entidades.AuthProvider.GOOGLE);
                    return repo.save(nuevo);
                });
    }

    public Usuario obtenerPorEmail(String email) {
        return repo.findByEmail(email)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));
    }

    public Usuario obtenerPorId(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));
    }

    public Usuario editarPerfil(Long id, String nuevoNombre, String nuevoEmail,
                                String nuevoDni, String nuevoTelefono, String nuevaPassword) {
        Usuario usuario = obtenerPorId(id);
        if (nuevoNombre != null && !nuevoNombre.isBlank())     usuario.setNombre(nuevoNombre);
        if (nuevoEmail != null && !nuevoEmail.isBlank())       usuario.setEmail(nuevoEmail);
        if (nuevoDni != null && !nuevoDni.isBlank())           usuario.setDni(nuevoDni);
        if (nuevoTelefono != null && !nuevoTelefono.isBlank()) usuario.setTelefono(nuevoTelefono);
        if (nuevaPassword != null && !nuevaPassword.isBlank()) usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        return repo.save(usuario);
    }

    /**
     * Acredita al usuario como socio SOLO si el pago con tarjeta es aprobado.
     * Es @Transactional: si el pago se rechaza o los datos son inválidos, se lanza
     * IllegalStateException y no queda ningún cambio parcial (ni rol ni membresía).
     */
    @org.springframework.transaction.annotation.Transactional
    public Membresia acreditarSocio(Long id, com.sportsync.backend.dto.DatosTarjeta tarjeta) {
        Usuario usuario = obtenerPorId(id);
        if (usuario.getRol() == Rol.ADMIN)
            throw new IllegalStateException("Los administradores no pueden acreditarse como socios.");
        if (usuario.getRol() == Rol.SOCIO)
            throw new IllegalStateException("El usuario ya es socio.");

        // Cobro de la cuota de socio con la pasarela (antes de tocar el estado del usuario)
        com.sportsync.backend.dto.ResultadoPago resultado = pasarelaPago.cobrar(
                new com.sportsync.backend.dto.SolicitudPago(
                        java.math.BigDecimal.valueOf(cuotaSocio),
                        "Membresía de socio SportSync",
                        tarjeta));
        if (!resultado.fueAprobado()) {
            // Rechazo o datos inválidos → no se completa la inscripción (rollback)
            throw new IllegalStateException(resultado.getMensaje());
        }

        membresiaRepo.findByUsuarioIdAndEstado(id, EstadoMembresia.ACTIVA)
                .ifPresent(m -> { m.setEstado(EstadoMembresia.CANCELADA); membresiaRepo.save(m); });

        usuario.setRol(Rol.SOCIO);
        repo.save(usuario);

        Membresia membresia = new Membresia();
        membresia.setUsuario(usuario);
        membresia.setFechaInicio(LocalDate.now());
        membresia.setFechaVencimiento(LocalDate.now().plusYears(1));
        membresia.setEstado(EstadoMembresia.ACTIVA);
        return membresiaRepo.save(membresia);
    }

    public Usuario cancelarSocio(Long id) {
        Usuario usuario = obtenerPorId(id);
        if (usuario.getRol() != Rol.SOCIO)
            throw new IllegalStateException("El usuario no es socio.");

        membresiaRepo.findByUsuarioIdAndEstado(id, EstadoMembresia.ACTIVA)
                .ifPresent(m -> { m.setEstado(EstadoMembresia.CANCELADA); membresiaRepo.save(m); });

        usuario.setRol(Rol.NO_SOCIO);
        return repo.save(usuario);
    }

    public void eliminarUsuario(Long id) {
        if (!repo.existsById(id))
            throw new UsuarioNoEncontradoException("Usuario no encontrado.");
        repo.deleteById(id);
    }

    public Usuario suspender(Long id, String tipo, Integer dias) {
        Usuario usuario = obtenerPorId(id);

        suspensionRepo.findByUsuarioIdAndActivaTrue(id)
                .ifPresent(s -> { s.setActiva(false); suspensionRepo.save(s); });

        Suspension suspension = new Suspension();
        suspension.setUsuario(usuario);
        suspension.setFechaInicio(LocalDate.now());
        suspension.setActiva(true);

        if ("TEMPORAL".equals(tipo)) {
            if (dias == null || dias <= 0)
                throw new IllegalArgumentException("Días de suspensión inválidos.");
            suspension.setTipo(TipoSuspension.TEMPORAL);
            suspension.setFechaFin(LocalDate.now().plusDays(dias));
            usuario.setEstado(EstadoUsuario.SUSPENDIDO_TEMPORAL);
        } else {
            suspension.setTipo(TipoSuspension.PERMANENTE);
            suspension.setFechaFin(null);
            usuario.setEstado(EstadoUsuario.SUSPENDIDO_PERMANENTE);
        }

        suspensionRepo.save(suspension);
        return repo.save(usuario);
    }

    public Usuario rehabilitar(Long id) {
        Usuario usuario = obtenerPorId(id);

        suspensionRepo.findByUsuarioIdAndActivaTrue(id)
                .ifPresent(s -> { s.setActiva(false); suspensionRepo.save(s); });

        usuario.setEstado(EstadoUsuario.ACTIVO);
        return repo.save(usuario);
    }
}