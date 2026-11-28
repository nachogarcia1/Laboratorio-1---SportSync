package com.sportsync.backend.security;

import com.sportsync.backend.config.JwtUtil;
import com.sportsync.backend.model.admin.EstadoUsuario;
import com.sportsync.backend.model.entidades.Rol;
import com.sportsync.backend.model.entidades.Usuario;
import com.sportsync.backend.model.reserva.Reserva;
import com.sportsync.backend.repository.ReservaRepository;
import com.sportsync.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-03: un usuario autenticado no puede operar sobre reservas/pagos de otro.
 * Los repositorios de usuario y reserva están mockeados, así que no se escribe nada en la base
 * (igual el contexto de Spring necesita la base arriba, como {@code BackendApplicationTests}).
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AccesoCruzadoTest {

    private static final long ID_ALICE = 1L;
    private static final long ID_BOB = 2L;
    private static final long ID_RESERVA_DE_ALICE = 10L;

    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwtUtil;

    @MockitoBean UsuarioRepository usuarioRepo;
    @MockitoBean ReservaRepository reservaRepo;

    String tokenAlice;
    String tokenBob;
    String tokenAdmin;

    @BeforeEach
    void preparar() {
        Usuario alice = usuario(ID_ALICE, "alice@test.com", Rol.NO_SOCIO);
        Usuario bob = usuario(ID_BOB, "bob@test.com", Rol.NO_SOCIO);
        Usuario admin = usuario(99L, "admin@test.com", Rol.ADMIN);

        when(usuarioRepo.findByEmail("alice@test.com")).thenReturn(Optional.of(alice));
        when(usuarioRepo.findByEmail("bob@test.com")).thenReturn(Optional.of(bob));
        when(usuarioRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        Reserva reserva = new Reserva();
        ReflectionTestUtils.setField(reserva, "id", ID_RESERVA_DE_ALICE);
        reserva.setUsuario(alice);
        when(reservaRepo.findById(ID_RESERVA_DE_ALICE)).thenReturn(Optional.of(reserva));
        when(reservaRepo.findById(404L)).thenReturn(Optional.empty());
        when(reservaRepo.findByUsuarioId(any())).thenReturn(java.util.List.of());

        tokenAlice = jwtUtil.generarToken("alice@test.com", "NO_SOCIO");
        tokenBob = jwtUtil.generarToken("bob@test.com", "NO_SOCIO");
        tokenAdmin = jwtUtil.generarToken("admin@test.com", "ADMIN");
    }

    private static Usuario usuario(Long id, String email, Rol rol) {
        Usuario u = new Usuario();
        ReflectionTestUtils.setField(u, "id", id);
        u.setEmail(email);
        u.setRol(rol);
        u.setEstado(EstadoUsuario.ACTIVO);
        return u;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    // ── GET /reservas/usuario/{id} ────────────────────────────────────────────

    @Test
    void historial_propio_ok() throws Exception {
        mvc.perform(get("/reservas/usuario/" + ID_ALICE).header("Authorization", bearer(tokenAlice)))
                .andExpect(status().isOk());
    }

    @Test
    void historial_ajeno_403() throws Exception {
        mvc.perform(get("/reservas/usuario/" + ID_ALICE).header("Authorization", bearer(tokenBob)))
                .andExpect(status().isForbidden());
    }

    @Test
    void historial_ajeno_como_admin_ok() throws Exception {
        mvc.perform(get("/reservas/usuario/" + ID_ALICE).header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isOk());
    }

    @Test
    void historial_sin_token_denegado() throws Exception {
        int status = mvc.perform(get("/reservas/usuario/" + ID_ALICE)).andReturn().getResponse().getStatus();
        assertNotEquals(200, status);
    }

    // ── PUT /reservas/{id}/cancelar ───────────────────────────────────────────

    @Test
    void cancelar_reserva_ajena_403_aunque_el_body_diga_que_es_suya() throws Exception {
        // Bob manda usuarioId = Alice (la dueña) en el body: igual se rechaza, el usuario sale del JWT.
        mvc.perform(put("/reservas/" + ID_RESERVA_DE_ALICE + "/cancelar")
                        .header("Authorization", bearer(tokenBob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + ID_ALICE + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelar_reserva_inexistente_403_sin_revelar_existencia() throws Exception {
        mvc.perform(put("/reservas/404/cancelar")
                        .header("Authorization", bearer(tokenBob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + ID_BOB + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelar_reserva_propia_no_es_403() throws Exception {
        int status = mvc.perform(put("/reservas/" + ID_RESERVA_DE_ALICE + "/cancelar")
                        .header("Authorization", bearer(tokenAlice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn().getResponse().getStatus();
        assertNotEquals(403, status);
    }

    // ── /pagos/* ──────────────────────────────────────────────────────────────

    @Test
    void iniciar_pago_a_nombre_de_otro_403() throws Exception {
        String body = "{\"reserva\":{\"usuarioId\":" + ID_ALICE + ",\"canchaId\":1,"
                + "\"fecha\":\"2026-12-01\",\"horaInicio\":\"18:00\",\"horaFin\":\"19:00\"},\"metodo\":\"TARJETA_CREDITO\"}";
        mvc.perform(post("/pagos/iniciar")
                        .header("Authorization", bearer(tokenBob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void iniciar_pago_sin_usuarioId_403() throws Exception {
        mvc.perform(post("/pagos/iniciar")
                        .header("Authorization", bearer(tokenBob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reserva\":{\"canchaId\":1},\"metodo\":\"TARJETA_CREDITO\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void pagar_con_tarjeta_reserva_ajena_403() throws Exception {
        mvc.perform(post("/pagos/tarjeta?reservaId=" + ID_RESERVA_DE_ALICE)
                        .header("Authorization", bearer(tokenBob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirmar_pago_reserva_ajena_403() throws Exception {
        mvc.perform(post("/pagos/confirmar?reservaId=" + ID_RESERVA_DE_ALICE)
                        .header("Authorization", bearer(tokenBob)))
                .andExpect(status().isForbidden());
    }

    @Test
    void simular_pago_reserva_ajena_403() throws Exception {
        mvc.perform(post("/pagos/simular?reservaId=" + ID_RESERVA_DE_ALICE + "&resultado=APROBADO")
                        .header("Authorization", bearer(tokenBob)))
                .andExpect(status().isForbidden());
    }

    @Test
    void estado_de_pago_reserva_ajena_403() throws Exception {
        mvc.perform(get("/pagos/reserva/" + ID_RESERVA_DE_ALICE).header("Authorization", bearer(tokenBob)))
                .andExpect(status().isForbidden());
    }

    @Test
    void estado_de_pago_reserva_ajena_como_admin_no_es_403() throws Exception {
        ResultActions r = mvc.perform(get("/pagos/reserva/" + ID_RESERVA_DE_ALICE)
                .header("Authorization", bearer(tokenAdmin)));
        assertNotEquals(403, r.andReturn().getResponse().getStatus());
    }

    @Test
    void estado_de_pago_reserva_propia_no_es_403() throws Exception {
        ResultActions r = mvc.perform(get("/pagos/reserva/" + ID_RESERVA_DE_ALICE)
                .header("Authorization", bearer(tokenAlice)));
        assertNotEquals(403, r.andReturn().getResponse().getStatus());
    }
}
