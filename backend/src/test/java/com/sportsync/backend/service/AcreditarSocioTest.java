package com.sportsync.backend.service;

import com.sportsync.backend.dto.DatosTarjeta;
import com.sportsync.backend.dto.ResultadoPago;
import com.sportsync.backend.model.admin.EstadoMembresia;
import com.sportsync.backend.model.admin.Membresia;
import com.sportsync.backend.model.entidades.Rol;
import com.sportsync.backend.model.entidades.Usuario;
import com.sportsync.backend.repository.MembresiaRepository;
import com.sportsync.backend.repository.SuspensionRepository;
import com.sportsync.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Inscripción a socio: solo se acredita si el pago con tarjeta es aprobado. */
class AcreditarSocioTest {

    private UsuarioRepository repo;
    private MembresiaRepository membresiaRepo;
    private SuspensionRepository suspensionRepo;
    private PasarelaPago pasarela;
    private UsuarioService service;
    private Usuario usuario;

    @BeforeEach
    void setup() {
        repo = mock(UsuarioRepository.class);
        membresiaRepo = mock(MembresiaRepository.class);
        suspensionRepo = mock(SuspensionRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        pasarela = mock(PasarelaPago.class);
        service = new UsuarioService(repo, encoder, suspensionRepo, membresiaRepo, pasarela);

        usuario = new Usuario();
        usuario.setEmail("test@mail.com");
        usuario.setRol(Rol.NO_SOCIO);
        when(repo.findById(1L)).thenReturn(Optional.of(usuario));
        when(membresiaRepo.findByUsuarioIdAndEstado(1L, EstadoMembresia.ACTIVA)).thenReturn(Optional.empty());
        when(membresiaRepo.save(ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
    }

    private DatosTarjeta tarjeta() {
        DatosTarjeta t = new DatosTarjeta();
        t.setTitular("Juan Perez"); t.setNumero("4111111111111111"); t.setCvv("123"); t.setVencimiento("12/30");
        return t;
    }

    @Test
    void inscripcionExitosaCuandoElPagoEsAprobado() {
        when(pasarela.cobrar(ArgumentMatchers.any()))
                .thenReturn(ResultadoPago.aprobado("MOCK-1", "1111", "ok"));

        Membresia m = service.acreditarSocio(1L, tarjeta());

        assertEquals(Rol.SOCIO, usuario.getRol());          // pasó a socio
        assertEquals(EstadoMembresia.ACTIVA, m.getEstado()); // membresía activa
        verify(repo).save(usuario);
        verify(membresiaRepo).save(ArgumentMatchers.any());
    }

    @Test
    void inscripcionRechazadaCuandoFallaElPago() {
        when(pasarela.cobrar(ArgumentMatchers.any()))
                .thenReturn(ResultadoPago.rechazado("0002", "Pago rechazado por el emisor."));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.acreditarSocio(1L, tarjeta()));

        assertTrue(ex.getMessage().toLowerCase().contains("rechaz"));
        // No quedó ningún cambio parcial: ni socio ni membresía
        assertEquals(Rol.NO_SOCIO, usuario.getRol());
        verify(repo, never()).save(ArgumentMatchers.any());
        verify(membresiaRepo, never()).save(ArgumentMatchers.any());
    }

    @Test
    void datosInvalidosNoAcreditan() {
        when(pasarela.cobrar(ArgumentMatchers.any()))
                .thenReturn(ResultadoPago.datosInvalidos("Número de tarjeta inválido."));

        assertThrows(IllegalStateException.class, () -> service.acreditarSocio(1L, tarjeta()));
        assertEquals(Rol.NO_SOCIO, usuario.getRol());
        verify(membresiaRepo, never()).save(ArgumentMatchers.any());
    }
}
