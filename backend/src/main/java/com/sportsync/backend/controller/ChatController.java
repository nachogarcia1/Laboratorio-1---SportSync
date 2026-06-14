package com.sportsync.backend.controller;

import com.sportsync.backend.dto.ChatMensajeDTO;
import com.sportsync.backend.dto.ConversacionResumenDTO;
import com.sportsync.backend.model.chat.RemitenteChat;
import com.sportsync.backend.service.ChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    // Usuario envía mensaje
    @MessageMapping("/chat/enviar")
    public void recibirMensajeUsuario(@Payload Map<String, Object> payload) {
        Long usuarioId = Long.valueOf(payload.get("usuarioId").toString());
        String contenido = payload.get("contenido").toString();
        ChatMensajeDTO msg = chatService.guardarMensaje(usuarioId, contenido, RemitenteChat.USUARIO);
        messagingTemplate.convertAndSend("/topic/chat/admin", msg);
        messagingTemplate.convertAndSend("/topic/chat/usuario/" + usuarioId, msg);
    }

    // Admin responde
    @MessageMapping("/chat/responder")
    public void recibirRespuestaAdmin(@Payload Map<String, Object> payload) {
        Long usuarioId = Long.valueOf(payload.get("usuarioId").toString());
        String contenido = payload.get("contenido").toString();
        ChatMensajeDTO msg = chatService.guardarMensaje(usuarioId, contenido, RemitenteChat.ADMIN);
        messagingTemplate.convertAndSend("/topic/chat/usuario/" + usuarioId, msg);
        messagingTemplate.convertAndSend("/topic/chat/admin", msg);
    }

    @GetMapping("/historial/{usuarioId}")
    public List<ChatMensajeDTO> historial(@PathVariable Long usuarioId) {
        return chatService.getHistorial(usuarioId);
    }

    @GetMapping("/conversaciones")
    public List<ConversacionResumenDTO> conversaciones() {
        return chatService.getConversaciones();
    }

    @PostMapping("/leer/{usuarioId}")
    public void marcarLeidos(@PathVariable Long usuarioId) {
        chatService.marcarLeidos(usuarioId);
    }
}