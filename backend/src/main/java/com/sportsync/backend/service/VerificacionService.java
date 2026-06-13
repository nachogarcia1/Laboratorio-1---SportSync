package com.sportsync.backend.service;

import com.sportsync.backend.exception.UsuarioNoEncontradoException;
import com.sportsync.backend.model.entidades.CodigoVerificacion;
import com.sportsync.backend.model.entidades.Usuario;
import com.sportsync.backend.repository.CodigoVerificacionRepository;
import com.sportsync.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Gestión del código de verificación de email para cuentas locales.
 * Reglas: código de 6 dígitos, vencimiento configurable, máximo de intentos
 * fallidos y rate-limit en el reenvío.
 */
@Service
public class VerificacionService {

    private final CodigoVerificacionRepository codigoRepo;
    private final UsuarioRepository usuarioRepo;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    @Value("${sportsync.verificacion.expiracion-minutos:15}")
    private int expiracionMinutos;

    @Value("${sportsync.verificacion.max-intentos:5}")
    private int maxIntentos;

    @Value("${sportsync.verificacion.reenvio-segundos:60}")
    private int reenvioSegundos;

    public VerificacionService(CodigoVerificacionRepository codigoRepo,
                               UsuarioRepository usuarioRepo,
                               EmailService emailService) {
        this.codigoRepo = codigoRepo;
        this.usuarioRepo = usuarioRepo;
        this.emailService = emailService;
    }

    /** Genera y envía un código nuevo para el usuario, descartando los previos. */
    @Transactional
    public void generarYEnviar(Usuario usuario) {
        codigoRepo.deleteByUsuarioId(usuario.getId());

        String codigo = String.format("%06d", random.nextInt(1_000_000));
        CodigoVerificacion cv = new CodigoVerificacion();
        cv.setUsuario(usuario);
        cv.setCodigo(codigo);
        cv.setExpiraEn(LocalDateTime.now().plusMinutes(expiracionMinutos));
        codigoRepo.save(cv);

        emailService.enviarCodigoVerificacion(usuario.getEmail(), codigo, expiracionMinutos);
    }

    /** Reenvío con rate-limit: no permite pedir otro código antes de reenvioSegundos. */
    @Transactional
    public void reenviar(String email) {
        Usuario usuario = usuarioRepo.findByEmail(email)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));
        if (usuario.isVerificado())
            throw new IllegalStateException("La cuenta ya está verificada.");

        Optional<CodigoVerificacion> ultimo =
                codigoRepo.findTopByUsuarioIdOrderByCreatedAtDesc(usuario.getId());
        if (ultimo.isPresent()) {
            long seg = java.time.Duration.between(ultimo.get().getCreatedAt(), LocalDateTime.now()).getSeconds();
            if (seg < reenvioSegundos)
                throw new IllegalStateException("Esperá " + (reenvioSegundos - seg) + " segundos para reenviar el código.");
        }
        generarYEnviar(usuario);
    }

    /**
     * Valida el código ingresado. Marca al usuario como verificado si es correcto.
     * @return el usuario verificado
     */
    @Transactional
    public Usuario verificar(String email, String codigoIngresado) {
        Usuario usuario = usuarioRepo.findByEmail(email)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));
        if (usuario.isVerificado())
            return usuario; // idempotente

        CodigoVerificacion cv = codigoRepo.findTopByUsuarioIdOrderByCreatedAtDesc(usuario.getId())
                .orElseThrow(() -> new IllegalStateException("No hay un código pendiente. Pedí uno nuevo."));

        if (!cv.esVigente())
            throw new IllegalStateException("El código venció. Pedí uno nuevo.");

        if (cv.getIntentos() >= maxIntentos)
            throw new IllegalStateException("Superaste el máximo de intentos. Pedí un código nuevo.");

        if (!cv.getCodigo().equals(codigoIngresado)) {
            cv.setIntentos(cv.getIntentos() + 1);
            codigoRepo.save(cv);
            int restantes = maxIntentos - cv.getIntentos();
            throw new IllegalArgumentException("Código incorrecto. Te quedan " + Math.max(restantes, 0) + " intentos.");
        }

        cv.setConsumido(true);
        codigoRepo.save(cv);
        usuario.setVerificado(true);
        return usuarioRepo.save(usuario);
    }
}
