package com.sportsync.backend.config;

import com.sportsync.backend.dto.IniciarPagoRequest;
import com.sportsync.backend.model.entidades.Usuario;
import com.sportsync.backend.repository.ReservaRepository;
import com.sportsync.backend.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Reglas de propiedad de recursos, usadas desde {@code @PreAuthorize("@authz...")}.
 * La identidad sale siempre del JWT (el principal que arma {@link JwtFilter} es el email),
 * nunca de un id que mande el cliente.
 */
@Component("authz")
public class Autorizacion {

    private final UsuarioRepository usuarioRepo;
    private final ReservaRepository reservaRepo;

    public Autorizacion(UsuarioRepository usuarioRepo, ReservaRepository reservaRepo) {
        this.usuarioRepo = usuarioRepo;
        this.reservaRepo = reservaRepo;
    }

    /** Id del usuario autenticado, o null si no hay sesión válida. */
    public Long idActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) return null;
        return usuarioRepo.findByEmail(auth.getName()).map(Usuario::getId).orElse(null);
    }

    public boolean esAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(a.getAuthority())) return true;
        }
        return false;
    }

    /** El id recibido es el del usuario autenticado. */
    public boolean esUsuario(Long usuarioId) {
        Long actual = idActual();
        return usuarioId != null && actual != null && usuarioId.equals(actual);
    }

    public boolean esUsuarioOAdmin(Long usuarioId) {
        return esAdmin() || esUsuario(usuarioId);
    }

    /** El usuario autenticado es el dueño de la reserva. Si la reserva no existe, deniega (no revela existencia). */
    public boolean esDuenioDeReserva(Long reservaId) {
        Long actual = idActual();
        if (reservaId == null || actual == null) return false;
        return reservaRepo.findById(reservaId)
                .map(r -> r.getUsuario() != null && actual.equals(r.getUsuario().getId()))
                .orElse(false);
    }

    public boolean esDuenioDeReservaOAdmin(Long reservaId) {
        return esAdmin() || esDuenioDeReserva(reservaId);
    }

    /** El usuario de la reserva que se quiere crear al iniciar el pago es el autenticado. */
    public boolean puedeIniciarPago(IniciarPagoRequest req) {
        return req != null && req.getReserva() != null && esUsuario(req.getReserva().getUsuarioId());
    }
}
