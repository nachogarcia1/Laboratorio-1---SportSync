package com.sportsync.backend.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsync.backend.config.JwtUtil;
import com.sportsync.backend.model.entidades.Rol;
import com.sportsync.backend.model.entidades.Usuario;
import com.sportsync.backend.repository.UsuarioRepository;
import com.sportsync.backend.testsupport.BaseDeDatosTemporal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Chat usuario↔admin de punta a punta: WebSocket/STOMP real + REST (historial y conversaciones del admin). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatTiempoRealTest {

    private static final BaseDeDatosTemporal BD = BaseDeDatosTemporal.crear("chat");
    private static final ObjectMapper JSON = new ObjectMapper();

    @DynamicPropertySource
    static void baseTemporal(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", BD::url);
        r.add("spring.datasource.username", BD::usuario);
        r.add("spring.datasource.password", BD::clave);
    }

    @LocalServerPort int puerto;
    @Autowired JwtUtil jwtUtil;
    @Autowired UsuarioRepository usuarioRepo;

    private Usuario cliente;
    private Usuario admin;

    @BeforeAll
    void sembrar() {
        cliente = nuevo("cliente@prueba.com", "30000001", Rol.NO_SOCIO);
        admin = nuevo("admin@prueba.com", "30000002", Rol.ADMIN);
    }

    private Usuario nuevo(String email, String dni, Rol rol) {
        Usuario u = new Usuario();
        u.setNombre(rol.name());
        u.setEmail(email);
        u.setPassword("no-importa");
        u.setDni(dni);
        u.setTelefono("11" + dni);
        u.setRol(rol);
        u.setVerificado(true);
        return usuarioRepo.save(u);
    }

    private StompSession conectar() throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new StringMessageConverter() {
            @Override protected boolean supportsMimeType(org.springframework.messaging.MessageHeaders h) { return true; }
        });
        return client.connectAsync("ws://localhost:" + puerto + "/ws", new WebSocketHttpHeaders(),
                new StompSessionHandlerAdapter() {}).get(10, TimeUnit.SECONDS);
    }

    /** Envía JSON como lo hace el frontend (content-type application/json). */
    private void enviar(StompSession s, String destino, String json) {
        StompHeaders h = new StompHeaders();
        h.setDestination(destino);
        h.setContentType(org.springframework.util.MimeTypeUtils.APPLICATION_JSON);
        s.send(h, json);
    }

    private BlockingQueue<String> suscribir(StompSession s, String destino) {
        BlockingQueue<String> cola = new LinkedBlockingQueue<>();
        s.subscribe(destino, new StompFrameHandler() {
            public Type getPayloadType(StompHeaders h) { return String.class; }
            public void handleFrame(StompHeaders h, Object body) { cola.add((String) body); }
        });
        return cola;
    }

    private HttpResponse<String> get(Usuario como, String ruta) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                .header("Authorization", "Bearer " + jwtUtil.generarToken(como.getEmail(), como.getRol().name()))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void el_mensaje_del_usuario_le_llega_al_admin_y_la_respuesta_del_admin_al_usuario() throws Exception {
        StompSession sUsuario = conectar();
        StompSession sAdmin = conectar();
        BlockingQueue<String> alUsuario = suscribir(sUsuario, "/topic/chat/usuario/" + cliente.getId());
        BlockingQueue<String> alAdmin = suscribir(sAdmin, "/topic/chat/admin");
        Thread.sleep(500);   // dar tiempo a que las suscripciones queden registradas

        enviar(sUsuario, "/app/chat/enviar", "{\"usuarioId\":" + cliente.getId() + ",\"contenido\":\"hola soporte\"}");
        String recibidoPorAdmin = alAdmin.poll(10, TimeUnit.SECONDS);
        assertNotNull(recibidoPorAdmin, "el admin no recibió el mensaje del usuario");
        assertEquals("hola soporte", JSON.readTree(recibidoPorAdmin).get("contenido").asText());

        enviar(sAdmin, "/app/chat/responder", "{\"usuarioId\":" + cliente.getId() + ",\"contenido\":\"hola, te leo\"}");
        // el usuario recibe su propio mensaje y luego la respuesta
        String a = alUsuario.poll(10, TimeUnit.SECONDS);
        String b = alUsuario.poll(10, TimeUnit.SECONDS);
        assertNotNull(a);
        assertNotNull(b, "el usuario no recibió la respuesta del admin");
        assertEquals("hola, te leo", JSON.readTree(b).get("contenido").asText());
        assertEquals("ADMIN", JSON.readTree(b).get("remitente").asText());

        sUsuario.disconnect();
        sAdmin.disconnect();
    }

    @Test
    void el_historial_y_las_conversaciones_responden_ok_para_el_admin() throws Exception {
        StompSession s = conectar();
        enviar(s, "/app/chat/enviar", "{\"usuarioId\":" + cliente.getId() + ",\"contenido\":\"consulta\"}");
        Thread.sleep(1000);
        s.disconnect();

        HttpResponse<String> historial = get(admin, "/chat/historial/" + cliente.getId());
        assertEquals(200, historial.statusCode(), historial.body());
        assertTrue(JSON.readTree(historial.body()).size() >= 1);

        HttpResponse<String> conv = get(admin, "/chat/conversaciones");
        assertEquals(200, conv.statusCode(), conv.body());
        JsonNode arr = JSON.readTree(conv.body());
        assertTrue(arr.size() >= 1, "debe haber al menos una conversación");
        assertEquals(cliente.getId(), arr.get(0).get("usuarioId").asLong());
    }

    @Test
    void un_usuario_comun_no_puede_listar_las_conversaciones() throws Exception {
        assertEquals(403, get(cliente, "/chat/conversaciones").statusCode());
    }
}
